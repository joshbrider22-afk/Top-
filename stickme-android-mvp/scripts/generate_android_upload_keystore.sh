#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

ALIAS="${ANDROID_KEY_ALIAS:-upload}"
STORE_PASSWORD="${ANDROID_KEYSTORE_PASSWORD:?Set ANDROID_KEYSTORE_PASSWORD before running}"
KEY_PASSWORD="${ANDROID_KEY_PASSWORD:?Set ANDROID_KEY_PASSWORD before running}"
KEYSTORE_PATH="secrets/stickme-upload-key.jks"

mkdir -p secrets

keytool -genkeypair \
  -v \
  -storetype JKS \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=StickMe, OU=Mobile, O=TOPSHELFNZ, L=Auckland, ST=Auckland, C=NZ"

cat > keystore.properties <<EOF2
storeFile=$KEYSTORE_PATH
storePassword=$STORE_PASSWORD
keyAlias=$ALIAS
keyPassword=$KEY_PASSWORD
EOF2

if base64 --help 2>&1 | grep -q -- '-w'; then
  base64 -w 0 "$KEYSTORE_PATH" > android-keystore-base64.txt
else
  base64 "$KEYSTORE_PATH" | tr -d '\n' > android-keystore-base64.txt
fi

echo "Upload keystore created. Add android-keystore-base64.txt and password values to GitHub Actions secrets."
