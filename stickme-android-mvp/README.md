# StickMe Android Keyboard MVP

**Mission:** Photo → Sticker → Local Storage → Android Keyboard → Message

This folder contains the GitHub-buildable StickMe Android keyboard MVP plus the AAA App Store Experience Gate control layer.

## Build status

- Android Gradle project committed.
- Kotlin app shell committed.
- Local sticker storage committed.
- Android custom keyboard service committed.
- Sticker grid keyboard committed.
- `InputConnectionCompat` sticker insertion committed.
- GitHub Actions Android CI committed.
- Supabase App Store Experience Gate migration committed.
- Admin-only Founder War Room modules committed.
- Store Gate tests and preflight script committed.

## Android build command

```bash
gradle :app:assembleDebug --stacktrace
```

## Store gate commands

```bash
npm run test:app-store-gate
npm run preflight:app-store-gate
npm run store:ready
npm run final:store-ready
```

## App Store Experience Gate

The gate blocks release when:

- Consumer Home is disabled.
- Maintenance mode is active.
- Remote store review mode is enabled.
- Production Launch Lock is disabled.
- Privacy Policy URL is missing or invalid.
- Terms URL is missing or invalid.
- Support URL is missing or invalid.
- Account deletion URL is missing or invalid.

## Consumer experience rule

Home must stay sticker-first:

```text
Create sticker → Save to pack → Open keyboard → Share privately
```

Founder/admin dashboards belong behind admin-only access in Founder War Room.

## Acceptance test

A parent can create a sticker from a family photo, open the StickMe keyboard, tap the sticker, and get it into a message flow without backend, account, or internet.

## Physical QA still required

1. Run GitHub Actions / Android Studio build.
2. Install APK on a physical Android device.
3. Test Google Messages, WhatsApp, Telegram, Signal.
4. Confirm image insertion and fallback behaviour.
5. Create signed release keystore.
6. Build signed Android App Bundle.
7. Complete Google Play Data Safety and support/deletion URLs.
