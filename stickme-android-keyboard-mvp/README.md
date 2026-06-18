# StickMe Android Keyboard MVP

**Mission:** Photo → Sticker → Local Storage → Android Keyboard → Message

This repository section is the Android-first StickMe MVP execution package. It proves the core product loop without backend, accounts, payments, subscriptions, cloud sync, or team features.

## Current Scope

- Kotlin Android app
- Jetpack Compose app UI
- Local sticker storage
- PNG sticker export
- 160px thumbnail generation
- Android `InputMethodService` keyboard
- `FileProvider` secure content URI sharing
- `commitContent` compatible image insert attempt
- fallback text confirmation when a target app rejects image insertion

## Required Tools

- Android Studio
- Physical Android phone

Keyboard behavior must be validated on real hardware. Emulator checks are useful, but they are not a release gate.

## Build

1. Open this `stickme-android-keyboard-mvp` folder in Android Studio.
2. Let Gradle sync.
3. Build `app` debug.
4. Install on a physical Android phone.
5. Open the app once and create at least one sticker.
6. Enable the StickMe keyboard in Android input settings.
7. Test in Messages, WhatsApp, Telegram, and Signal.

## MVP Flow

1. Open StickMe.
2. Tap **Create Sticker**.
3. Select a photo.
4. Save sticker locally.
5. Confirm sticker appears in **My Stickers**.
6. Enable StickMe Keyboard.
7. Open a messaging app.
8. Switch to StickMe Keyboard.
9. Tap a sticker.
10. Confirm the sticker enters the message flow or fallback text appears.

## Performance Targets

| Gate | Target |
|---|---:|
| Keyboard opens | under 1 second |
| Sticker grid loads | under 1 second |
| Sticker tap action | under 2 seconds |
| Photo to sticker save | under 60 seconds |
| Offline operation | required |

## Release Rule

Do not add backend, accounts, cloud sync, or subscriptions until the keyboard loop is proven on a real Android phone.

The product is real when:

> A parent can turn one photo into a sticker and send it from the StickMe keyboard without help.
