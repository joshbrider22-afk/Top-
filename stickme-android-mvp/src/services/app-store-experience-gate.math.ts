import type {
  AppStoreExperienceGateStatus,
  AppStoreExperienceIssue,
} from "../types/app-store-experience-gate";

export function isPublicHttpsUrl(value: string): boolean {
  return /^https:\/\/.+\..+/.test(value.trim());
}

export function isPlaceholderUrl(value: string): boolean {
  const normalized = value.toLowerCase();

  return (
    normalized.includes("your_") ||
    normalized.includes("example.") ||
    normalized.includes("localhost") ||
    normalized.includes("placeholder")
  );
}

export function deriveAppStoreExperienceGate(input: {
  consumerHomeEnabled: boolean;
  maintenanceMode: boolean;
  remoteStoreReviewModeEnabled: boolean;
  productionLaunchLockEnabled: boolean;
  privacyPolicyUrl: string;
  termsUrl: string;
  supportUrl: string;
  accountDeletionUrl: string;
}): {
  status: AppStoreExperienceGateStatus;
  blockers: AppStoreExperienceIssue[];
} {
  const blockers: AppStoreExperienceIssue[] = [];

  if (!input.consumerHomeEnabled) {
    blockers.push({
      code: "consumer_home_disabled",
      title: "Consumer Home is disabled",
      detail: "Users must land on the sticker-first app experience.",
    });
  }

  if (input.maintenanceMode) {
    blockers.push({
      code: "maintenance_mode_active",
      title: "Maintenance Mode is active",
      detail: "Disable maintenance mode before submission.",
    });
  }

  if (input.remoteStoreReviewModeEnabled) {
    blockers.push({
      code: "remote_store_review_mode_enabled",
      title: "Remote Store Review Mode is enabled",
      detail: "Remote review mode must be off for production.",
    });
  }

  if (!input.productionLaunchLockEnabled) {
    blockers.push({
      code: "production_launch_lock_disabled",
      title: "Production Launch Lock is disabled",
      detail: "Release safety gate must stay enabled.",
    });
  }

  const urls = [
    ["privacy_policy_url_missing", "Privacy Policy URL is invalid", input.privacyPolicyUrl],
    ["terms_url_missing", "Terms URL is invalid", input.termsUrl],
    ["support_url_missing", "Support URL is invalid", input.supportUrl],
    ["account_deletion_url_missing", "Account deletion URL is invalid", input.accountDeletionUrl],
  ] as const;

  for (const [code, title, url] of urls) {
    if (!isPublicHttpsUrl(url) || isPlaceholderUrl(url)) {
      blockers.push({
        code,
        title,
        detail: "Use a real public HTTPS URL, not a placeholder.",
      });
    }
  }

  return {
    status: blockers.length === 0 ? "passed" : "blocked",
    blockers,
  };
}
