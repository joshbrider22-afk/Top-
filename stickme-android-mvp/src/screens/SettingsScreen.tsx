import { Linking } from "react-native";
import { ExperienceCard, ExperienceShell } from "../components/ExperienceShell";

const PRIVACY_URL = "https://topshelfnz.com/stickme/privacy";
const TERMS_URL = "https://topshelfnz.com/stickme/terms";
const SUPPORT_URL = "https://topshelfnz.com/stickme/support";
const ACCOUNT_DELETION_URL = "https://topshelfnz.com/stickme/delete-account";

export function SettingsScreen() {
  return (
    <ExperienceShell
      kicker="Trust centre"
      title="Privacy, support, and account controls."
      subtitle="Store-review ready links for user trust, safety, support, and deletion requirements."
    >
      <ExperienceCard title="Privacy Policy" body={PRIVACY_URL} onPress={() => void Linking.openURL(PRIVACY_URL)} />
      <ExperienceCard title="Terms" body={TERMS_URL} onPress={() => void Linking.openURL(TERMS_URL)} />
      <ExperienceCard title="Support" body={SUPPORT_URL} onPress={() => void Linking.openURL(SUPPORT_URL)} />
      <ExperienceCard title="Delete Account" body={ACCOUNT_DELETION_URL} tone="red" onPress={() => void Linking.openURL(ACCOUNT_DELETION_URL)} />
    </ExperienceShell>
  );
}
