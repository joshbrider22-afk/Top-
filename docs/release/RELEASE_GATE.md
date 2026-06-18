# Release Gate — AAA Binary Checklist

## Build gate

- [ ] Clean checkout builds.
- [ ] Android `bundleRelease` succeeds.
- [ ] Android `assembleRelease` succeeds.
- [ ] iOS archive succeeds.
- [ ] iOS IPA export succeeds.
- [ ] CI artifacts retained.
- [ ] Version code/build number incremented.

## Functional gate

- [ ] Home loads in under 700 ms target on test devices.
- [ ] Photo picker works.
- [ ] Camera capture works.
- [ ] Files import works.
- [ ] Sticker cutout output is transparent PNG.
- [ ] White outline is clean at sticker edge.
- [ ] My World™ save works offline.
- [ ] Share sheet sends generated PNG.
- [ ] Android IME reads cached thumbnails under 1 second.
- [ ] iOS keyboard extension reads App Group cache under 1 second if enabled.

## Privacy/security gate

- [ ] No keystroke logging.
- [ ] No background capture.
- [ ] No hidden analytics in keyboard targets.
- [ ] No unnecessary Contacts/Location/Microphone permissions.
- [ ] Photo permission copy is clear.
- [ ] Privacy policy matches binary behavior.
- [ ] Google Play Data Safety matches actual SDKs and network calls.
- [ ] Apple App Privacy answers match actual SDKs and network calls.

## Store asset gate

- [ ] App icon legible at small sizes.
- [ ] Screenshots are generated from real app UI or faithful approved mockups.
- [ ] No unlicensed faces, child images, pet images, logos, or third-party brands.
- [ ] Support URL live.
- [ ] Privacy URL live.
- [ ] Terms URL live.
- [ ] Review notes explain keyboard behavior and privacy posture.

## Monetization gate

- [ ] First sticker creation remains free/unblocked.
- [ ] Paid upgrade appears only after value created.
- [ ] Product IDs configured if subscriptions/IAP exist.
- [ ] Sandbox purchase test passes if IAP included.
- [ ] Revenue event tracking does not collect private photo/sticker content.
