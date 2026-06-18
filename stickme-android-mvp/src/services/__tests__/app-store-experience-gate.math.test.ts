import { describe, expect, it } from "vitest";
import {
  deriveAppStoreExperienceGate,
  isPlaceholderUrl,
  isPublicHttpsUrl,
} from "../app-store-experience-gate.math";

describe("app store experience gate math", () => {
  it("accepts public https URLs", () => {
    expect(isPublicHttpsUrl("https://topshelfnz.com/stickme/privacy")).toBe(true);
  });

  it("rejects placeholder URLs", () => {
    expect(isPlaceholderUrl("https://example.com/privacy")).toBe(true);
    expect(isPlaceholderUrl("http://localhost:3000/privacy")).toBe(true);
  });

  it("passes when every release gate is clean", () => {
    const result = deriveAppStoreExperienceGate({
      consumerHomeEnabled: true,
      maintenanceMode: false,
      remoteStoreReviewModeEnabled: false,
      productionLaunchLockEnabled: true,
      privacyPolicyUrl: "https://topshelfnz.com/stickme/privacy",
      termsUrl: "https://topshelfnz.com/stickme/terms",
      supportUrl: "https://topshelfnz.com/stickme/support",
      accountDeletionUrl: "https://topshelfnz.com/stickme/delete-account",
    });

    expect(result.status).toBe("passed");
    expect(result.blockers).toHaveLength(0);
  });

  it("blocks review mode and missing URLs", () => {
    const result = deriveAppStoreExperienceGate({
      consumerHomeEnabled: true,
      maintenanceMode: false,
      remoteStoreReviewModeEnabled: true,
      productionLaunchLockEnabled: true,
      privacyPolicyUrl: "https://example.com/privacy",
      termsUrl: "https://topshelfnz.com/stickme/terms",
      supportUrl: "https://topshelfnz.com/stickme/support",
      accountDeletionUrl: "",
    });

    expect(result.status).toBe("blocked");
    expect(result.blockers.map((blocker) => blocker.code)).toContain("remote_store_review_mode_enabled");
    expect(result.blockers.map((blocker) => blocker.code)).toContain("privacy_policy_url_missing");
    expect(result.blockers.map((blocker) => blocker.code)).toContain("account_deletion_url_missing");
  });
});
