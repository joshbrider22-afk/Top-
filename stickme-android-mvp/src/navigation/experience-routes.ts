import { AppStoreExperienceGateScreen } from "../screens/AppStoreExperienceGateScreen";
import { FounderWarRoomScreen } from "../screens/FounderWarRoomScreen";
import { HomeScreen } from "../screens/HomeScreen";
import { LaunchLockScreen } from "../screens/LaunchLockScreen";
import { SettingsScreen } from "../screens/SettingsScreen";

export const stickmeExperienceRoutes = {
  home: {
    name: "Home",
    consumerVisible: true,
    component: HomeScreen,
  },
  settings: {
    name: "Settings",
    consumerVisible: true,
    component: SettingsScreen,
  },
  founderWarRoom: {
    name: "Founder War Room",
    consumerVisible: false,
    adminOnly: true,
    component: FounderWarRoomScreen,
  },
  appStoreExperienceGate: {
    name: "App Store Experience Gate",
    consumerVisible: false,
    adminOnly: true,
    component: AppStoreExperienceGateScreen,
  },
  launchLock: {
    name: "Launch Lock",
    consumerVisible: false,
    adminOnly: true,
    component: LaunchLockScreen,
  },
} as const;

export type StickMeExperienceRouteKey = keyof typeof stickmeExperienceRoutes;
