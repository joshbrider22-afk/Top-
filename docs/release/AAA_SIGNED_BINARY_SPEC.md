# AAA Signed Binary Spec

## Product identity

- App name: StickMe
- Brand owner: TOPSHELFNZ
- Positioning: Create fast. Save beautifully. Send instantly.
- Default content mode: real-life people, family, pets, sports, vehicles, business objects.
- Default sticker style: transparent PNG, clean white contour outline, subtle shadow, optional neon red glow.

## Binary outputs

| Platform | Output | Purpose | Signing |
|---|---|---|---|
| Android | `.aab` | Google Play release/internal testing | Release upload key |
| Android | `.apk` | Device smoke testing/direct install | Release key or internal key |
| iOS | `.xcarchive` | Xcode archive record | Apple distribution signing |
| iOS | `.ipa` | TestFlight/App Store upload | App Store export signing |

## Bundle IDs

```text
Android applicationId: nz.topshelfnz.stickme
iOS app bundle ID: nz.topshelfnz.stickme
iOS keyboard extension bundle ID: nz.topshelfnz.stickme.keyboard
App Group: group.nz.topshelfnz.stickme
```

## Required user flow in signed binary

1. Home screen opens without account signup.
2. User taps Create Sticker.
3. User imports photo/camera/file.
4. Sticker engine generates cutout PNG.
5. User saves to My World™.
6. Sticker appears in Share flow.
7. Keyboard extension/IME can access saved sticker cache.

## Keyboard binary rules

### iOS

- Must include a Next Keyboard/Globe switch affordance.
- Secure text fields and some host apps can force the system keyboard.
- No keystroke collection.
- No hidden network calls from keyboard.
- App Group access must be explicit and aligned with privacy copy.

### Android

- IME must be minimal and sticker-focused.
- No text logging.
- No clipboard scraping.
- No background network from the keyboard.
- Use app-owned sticker cache as read-only input.

## Store-safe release posture

- Privacy-first local storage for v1.
- No forced signup before first sticker creation.
- No public use of child/family faces without written consent.
- No monetization gate before first created sticker.
- Support, Privacy Policy, Terms URLs live before submission.
