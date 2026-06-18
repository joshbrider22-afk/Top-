import { ExperienceCard, ExperienceShell } from "../components/ExperienceShell";

export function LaunchLockScreen() {
  return (
    <ExperienceShell
      kicker="Launch Lock"
      title="Release only when the gates pass."
      subtitle="The public build remains locked until product, privacy, support, legal, and store readiness checks are clean."
    >
      <ExperienceCard title="Public release locked" body="Keep production gated until the App Store Experience Gate returns passed." tone="red" />
      <ExperienceCard title="Preview allowed" body="Internal preview can continue while blockers are fixed." />
      <ExperienceCard title="Submission command" body="Run final store-ready commands only after gate status is passed." tone="green" />
    </ExperienceShell>
  );
}
