# Apple App Privacy Draft — Local-first v1

Use this only if the signed binary matches the stated behavior.

## Core privacy claim

StickMe creates and stores personal stickers locally unless the user explicitly shares or enables future sync.

## Keyboard privacy copy

StickMe Keyboard is a sticker picker. It does not collect, store, sell, or transmit keystrokes. It reads only the sticker cache required to display the user's saved stickers.

## App Privacy categories to verify

- User Content: user-selected photos/stickers, local-first.
- Diagnostics: only if crash reporting enabled.
- Purchases: only if in-app purchases/subscriptions enabled.
- Identifiers/Usage Data: only if analytics/attribution SDKs are included.

## Review notes

Explain to App Review:

1. The container app creates stickers from user-selected photos.
2. The keyboard extension displays saved stickers.
3. The keyboard does not log text input.
4. Secure fields may fall back to system keyboard.
5. Photos are not uploaded in v1 unless the user explicitly shares through the system share sheet.
