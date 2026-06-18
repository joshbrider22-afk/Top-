# StickMe AAA Signed Binary Runbook

## Target artifact

The production Android target is a signed release app bundle:

- `app/build/outputs/bundle/release/app-release.aab` for Google Play
- `app/build/outputs/apk/release/app-release.apk` for physical-device smoke testing
- `stickme-release-SHA256SUMS.txt` for artifact integrity checks

## Current package identity

The Android release identity is locked to:

```text
com.stickme.app
```

Do not change this after the first Play Console upload. If the Play Console app record uses a different package name, change it before the first upload only.

## Required GitHub Actions secrets

Add these in GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

`ANDROID_KEY_ALIAS` can be `upload`.

## Generate upload key locally

From repo root:

```bash
cd stickme-android-mvp
ANDROID_KEYSTORE_PASSWORD='replace-me' \
ANDROID_KEY_PASSWORD='replace-me' \
ANDROID_KEY_ALIAS='upload' \
bash scripts/generate_android_upload_keystore.sh .
```

Then copy the one-line value from:

```text
stickme-android-mvp/android-keystore-base64.txt
```

into the GitHub secret `ANDROID_KEYSTORE_BASE64`.

## Build signed binary in GitHub

1. Open GitHub Actions.
2. Select **StickMe Android Signed Binary**.
3. Press **Run workflow**.
4. Enter the correct `version_code` and `version_name`.
5. Download the artifact named `stickme-android-signed-binary-<version_code>`.
6. Upload the `.aab` to Google Play Console internal testing first.
7. Install the `.apk` on a real Android device and run the smoke test.

## Physical-device smoke test

Pass criteria:

- App launches.
- Tap **Create demo sticker**.
- Saved sticker count increases.
- Tap **Enable StickMe Keyboard**.
- Enable StickMe in Android keyboard settings.
- Open Messages, WhatsApp, or another compatible editor.
- Switch to StickMe keyboard.
- Tap sticker.
- Sticker inserts, or the fallback text appears when the target app rejects rich content.

## Release gate

Do not upload to production unless all are true:

- Signed AAB verifies successfully.
- Signed APK verifies successfully.
- Physical-device smoke test passes.
- Google Play Data Safety matches the app behavior.
- Privacy policy URL is live.
- Support/contact URL or email is live.
- Store screenshots match the actual app binary.
