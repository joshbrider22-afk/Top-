import { useEffect, useState } from "react";
import { ActivityIndicator, View } from "react-native";
import { ExperienceCard, ExperienceGateRow, ExperienceShell } from "../components/ExperienceShell";
import { getAppStoreExperienceGateSummary } from "../services/app-store-experience-gate.service";
import type { AppStoreExperienceGateSummary } from "../types/app-store-experience-gate";

export function AppStoreExperienceGateScreen() {
  const [summary, setSummary] = useState<AppStoreExperienceGateSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        setSummary(await getAppStoreExperienceGateSummary());
      } catch (err) {
        setError(err instanceof Error ? err.message : "Gate unavailable.");
      }
    }

    void load();
  }, []);

  return (
    <ExperienceShell
      kicker="App Store Experience Gate"
      title="Submission is controlled by real blockers."
      subtitle="Legal, support, privacy, review mode, and launch lock are checked before release."
    >
      {!summary && !error ? <ActivityIndicator /> : null}
      {error ? <ExperienceCard title="Gate unavailable" body={error} tone="red" /> : null}
      {summary ? (
        <View style={{ gap: 12 }}>
          <ExperienceCard
            title={summary.status === "passed" ? "Ready" : "Blocked"}
            body={summary.nextAction}
            tone={summary.status === "passed" ? "green" : "red"}
          />
          <ExperienceGateRow label="Consumer Home" value="Sticker-first public home" passed={Boolean(summary.requiredChecks.consumerHomeEnabled)} />
          <ExperienceGateRow label="Maintenance Mode" value="Must be off" passed={Boolean(summary.requiredChecks.maintenanceModeOff)} />
          <ExperienceGateRow label="Remote Review Mode" value="Must be off" passed={Boolean(summary.requiredChecks.remoteStoreReviewModeOff)} />
          <ExperienceGateRow label="Production Launch Lock" value="Must stay enabled" passed={Boolean(summary.requiredChecks.productionLaunchLockEnabled)} />
          {summary.blockers.map((blocker) => (
            <ExperienceCard key={blocker.code} title={blocker.title} body={blocker.detail} tone="red" />
          ))}
        </View>
      ) : null}
    </ExperienceShell>
  );
}
