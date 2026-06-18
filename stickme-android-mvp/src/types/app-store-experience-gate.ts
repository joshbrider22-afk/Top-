export type AppStoreExperienceGateStatus = "passed" | "blocked";
export type AppStoreExperienceDecision = "ready" | "block";

export type AppStoreExperienceIssue = {
  code: string;
  title: string;
  detail: string;
};

export type AppStoreExperienceGateSummary = {
  checkedAt: string;
  status: AppStoreExperienceGateStatus;
  decision: AppStoreExperienceDecision;
  nextAction: string;
  blockerCount: number;
  warningCount: number;
  blockers: AppStoreExperienceIssue[];
  warnings: AppStoreExperienceIssue[];
  requiredChecks: Record<string, unknown>;
  latestProductionLaunchLock: Record<string, unknown>;
  latestClarityAudit: Record<string, unknown>;
  latestBoardroomSnapshot: Record<string, unknown>;
};

export type AppStoreExperienceGateEvent = {
  id: string;
  status: AppStoreExperienceGateStatus;
  decision: AppStoreExperienceDecision;
  blockerCount: number;
  warningCount: number;
  blockers: AppStoreExperienceIssue[];
  warnings: AppStoreExperienceIssue[];
  requiredChecks: Record<string, unknown>;
  createdAt: string;
};
