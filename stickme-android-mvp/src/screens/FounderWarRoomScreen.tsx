import { ExperienceCard, ExperienceShell } from "../components/ExperienceShell";
import { useAdminAccess } from "../hooks/use-admin-access";

export function FounderWarRoomScreen() {
  const admin = useAdminAccess();

  if (admin.loading) {
    return (
      <ExperienceShell kicker="Founder War Room" title="Checking access." subtitle="Admin controls are protected.">
        <ExperienceCard title="Loading" body="Verifying founder access." />
      </ExperienceShell>
    );
  }

  if (!admin.isAdmin) {
    return (
      <ExperienceShell kicker="Founder War Room" title="Admin only." subtitle="Consumer users should never see operational dashboard sprawl.">
        <ExperienceCard title="Access restricted" body="This cockpit is only available to admin users." tone="red" />
      </ExperienceShell>
    );
  }

  return (
    <ExperienceShell
      kicker="Founder War Room"
      title="Operate the launch without polluting Home."
      subtitle="Admin-only controls for gate status, release readiness, clarity audits, and launch lock."
    >
      <ExperienceCard title="App Store Experience Gate" body="Check privacy, support, legal, deletion, review mode, and launch lock." tone="green" />
      <ExperienceCard title="Launch Lock" body="Confirm production release is hard-gated until every blocker is cleared." />
      <ExperienceCard title="Boardroom Snapshot" body="Keep founder operations away from the family sticker experience." />
    </ExperienceShell>
  );
}
