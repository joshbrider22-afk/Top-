# Signing Secrets Required

## Android release secrets

Store these in CI secrets or local `stickme-android-mvp/keystore.properties`. Never commit real values.

```text
ANDROID_KEYSTORE_BASE64       # Base64-encoded .jks/.keystore for CI
ANDROID_KEYSTORE_PASSWORD     # Keystore password
ANDROID_KEY_ALIAS             # Upload key alias
ANDROID_KEY_PASSWORD          # Key password
ANDROID_PACKAGE_NAME          # nz.topshelfnz.stickme
GOOGLE_PLAY_SERVICE_JSON      # Optional: Play Console service account JSON for automated upload
```

## Android local keystore.properties

```properties
storeFile=secrets/stickme-upload-key.jks
storePassword=CHANGE_ME
keyAlias=upload
keyPassword=CHANGE_ME
```

## iOS/App Store Connect secrets

```text
APPLE_TEAM_ID                 # Apple Developer Team ID
IOS_APP_IDENTIFIER            # nz.topshelfnz.stickme
IOS_KEYBOARD_IDENTIFIER       # nz.topshelfnz.stickme.keyboard
APP_GROUP_IDENTIFIER          # group.nz.topshelfnz.stickme
ASC_KEY_ID                    # App Store Connect API key ID
ASC_ISSUER_ID                 # App Store Connect issuer ID
ASC_KEY_P8_BASE64             # Base64-encoded .p8 private key
MATCH_PASSWORD                # Optional if using fastlane match
MATCH_GIT_URL                 # Optional private cert repo
```

## Certificate/profile requirements

- Apple Distribution certificate.
- App Store provisioning profile for the container app.
- App Store provisioning profile for the keyboard extension.
- App Group entitlement enabled for both app and extension if shared sticker cache is used.

## Key security policy

- Do not share keystores, `.p12`, `.mobileprovision`, or `.p8` files in chat.
- Inject secrets into GitHub Actions, Bitrise, Codemagic, EAS, or local keychain.
- Rotate if exposed.
- Keep one offline backup of the Android upload key.
