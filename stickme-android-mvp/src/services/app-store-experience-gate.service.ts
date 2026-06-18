import { supabase } from "../lib/supabase";
import type {
  AppStoreExperienceGateEvent,
  AppStoreExperienceGateSummary,
  AppStoreExperienceIssue,
} from "../types/app-store-experience-gate";

type DbGateEvent = {
  id: string;
  status: "passed" | "blocked";
  decision: "ready" | "block";
  blocker_count: number;
  warning_count: number;
  blockers: AppStoreExperienceIssue[];
  warnings: AppStoreExperienceIssue[];
  required_checks: Record<string, unknown>;
  created_at: string;
};

export async function getAppStoreExperienceGateSummary(): Promise<AppStoreExperienceGateSummary> {
  const { data, error } = await supabase.rpc("get_stickme_app_store_experience_gate");

  if (error || !data) {
    throw new Error(error?.message ?? "App Store Experience Gate unavailable.");
  }

  return parseAppStoreExperienceGateSummary(data as Record<string, unknown>);
}

export async function captureAppStoreExperienceGateEvent(): Promise<string> {
  const { data, error } = await supabase.rpc("capture_stickme_app_store_experience_gate");

  if (error || !data) {
    throw new Error(error?.message ?? "Unable to capture experience gate event.");
  }

  return String(data);
}

export async function getAppStoreExperienceGateEvents(limit = 20): Promise<AppStoreExperienceGateEvent[]> {
  const { data, error } = await supabase
    .from("app_store_experience_gate_events")
    .select("id,status,decision,blocker_count,warning_count,blockers,warnings,required_checks,created_at")
    .order("created_at", { ascending: false })
    .limit(limit);

  if (error) {
    throw new Error(error.message);
  }

  return (data ?? []).map((event) => mapGateEvent(event as DbGateEvent));
}

export function parseAppStoreExperienceGateSummary(value: Record<string, unknown>): AppStoreExperienceGateSummary {
  return {
    checkedAt: typeof value.checkedAt === "string" ? value.checkedAt : new Date().toISOString(),
    status: value.status === "passed" ? "passed" : "blocked",
    decision: value.decision === "ready" ? "ready" : "block",
    nextAction: String(value.nextAction ?? "Fix app-store experience blockers."),
    blockerCount: readNumber(value.blockerCount),
    warningCount: readNumber(value.warningCount),
    blockers: parseIssues(value.blockers),
    warnings: parseIssues(value.warnings),
    requiredChecks: readObject(value.requiredChecks),
    latestProductionLaunchLock: readObject(value.latestProductionLaunchLock),
    latestClarityAudit: readObject(value.latestClarityAudit),
    latestBoardroomSnapshot: readObject(value.latestBoardroomSnapshot),
  };
}

function mapGateEvent(event: DbGateEvent): AppStoreExperienceGateEvent {
  return {
    id: event.id,
    status: event.status,
    decision: event.decision,
    blockerCount: event.blocker_count,
    warningCount: event.warning_count,
    blockers: event.blockers ?? [],
    warnings: event.warnings ?? [],
    requiredChecks: event.required_checks ?? {},
    createdAt: event.created_at,
  };
}

function parseIssues(value: unknown): AppStoreExperienceIssue[] {
  if (!Array.isArray(value)) return [];

  return value.map((issue) => {
    const item = readObject(issue);
    return {
      code: String(item.code ?? "unknown"),
      title: String(item.title ?? "Issue"),
      detail: String(item.detail ?? ""),
    };
  });
}

function readObject(value: unknown): Record<string, unknown> {
  if (typeof value === "object" && value !== null && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return {};
}

function readNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;

  if (typeof value === "string") {
    const parsed = Number.parseFloat(value);
    if (Number.isFinite(parsed)) return parsed;
  }

  return 0;
}
