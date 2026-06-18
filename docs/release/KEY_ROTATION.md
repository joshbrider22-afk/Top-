# Key Rotation and Recovery

## Android

- Keep the upload key offline-backed up.
- Store CI copy as Base64 secret only.
- Limit access to release maintainers.
- Rotate upload key through Play Console if compromised.
- Never commit `keystore.properties` or `.jks` files.

## iOS

- Prefer App Store Connect API key for upload automation.
- Store certificates/profiles with Fastlane Match in a private encrypted repo or manage manually in Xcode.
- Rotate App Store Connect API key if exposed.
- Remove old certificates from CI when replaced.
- Keep Team ID and bundle IDs stable.
