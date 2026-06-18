import { existsSync, readFileSync } from "node:fs";

const requiredFiles = [
  "supabase/migrations/035_aaa_app_store_experience_gate.sql",
  "src/types/app-store-experience-gate.ts",
  "src/services/app-store-experience-gate.math.ts",
  "src/services/app-store-experience-gate.service.ts",
  "src/hooks/use-admin-access.ts",
  "src/components/ExperienceShell.tsx",
  "src/screens/HomeScreen.tsx",
  "src/screens/SettingsScreen.tsx",
  "src/screens/FounderWarRoomScreen.tsx",
  "src/screens/AppStoreExperienceGateScreen.tsx",
  "src/screens/LaunchLockScreen.tsx",
];

const requiredUrls = [
  "https://topshelfnz.com/stickme/privacy",
  "https://topshelfnz.com/stickme/terms",
  "https://topshelfnz.com/stickme/support",
  "https://topshelfnz.com/stickme/delete-account",
];

const blockers = [];

for (const file of requiredFiles) {
  if (!existsSync(file)) {
    blockers.push(`Missing required file: ${file}`);
  }
}

for (const file of requiredFiles.filter((name) => existsSync(name))) {
  const content = readFileSync(file, "utf8");
  if (/localhost|example\.|placeholder|your_/i.test(content)) {
    blockers.push(`Placeholder content detected in ${file}`);
  }
}

const migration = existsSync(requiredFiles[0]) ? readFileSync(requiredFiles[0], "utf8") : "";
for (const url of requiredUrls) {
  if (!migration.includes(url)) {
    blockers.push(`Required public URL missing from migration: ${url}`);
  }
}

if (blockers.length > 0) {
  console.error("StickMe App Store Experience Gate blocked:");
  for (const blocker of blockers) console.error(`- ${blocker}`);
  process.exit(1);
}

console.log("StickMe App Store Experience Gate preflight passed.");
