# StickMe AAA App Store Experience Gate

## Purpose

This module removes founder/admin dashboard sprawl from the consumer Home experience and moves operational controls into an admin-only Founder War Room.

## Implemented gates

1. Consumer Home must be enabled.
2. Founder War Room is admin-only.
3. Maintenance mode must be off.
4. Remote store review mode must be off.
5. Production Launch Lock must remain enabled.
6. Privacy Policy URL must be public HTTPS.
7. Terms URL must be public HTTPS.
8. Support URL must be public HTTPS.
9. Account deletion URL must be public HTTPS.

## Database layer

Migration:

```text
supabase/migrations/035_aaa_app_store_experience_gate.sql
```

Functions:

```text
public.get_stickme_app_store_experience_gate()
public.capture_stickme_app_store_experience_gate()
```

Events table:

```text
public.app_store_experience_gate_events
```

## App layer

Portable React Native modules:

```text
src/components/ExperienceShell.tsx
src/hooks/use-admin-access.ts
src/screens/HomeScreen.tsx
src/screens/SettingsScreen.tsx
src/screens/FounderWarRoomScreen.tsx
src/screens/AppStoreExperienceGateScreen.tsx
src/screens/LaunchLockScreen.tsx
src/services/app-store-experience-gate.service.ts
src/services/app-store-experience-gate.math.ts
```

## Commands

```bash
npm run test:app-store-gate
npm run preflight:app-store-gate
npm run store:ready
npm run final:store-ready
```

## Release rule

Do not submit a production build unless the gate returns:

```text
status = passed
decision = ready
blockerCount = 0
```
