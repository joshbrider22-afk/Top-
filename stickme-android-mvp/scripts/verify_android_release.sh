#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

fail() { echo "❌ $1" >&2; exit 1; }
pass() { echo "✅ $1"; }

AAB_DIR="app/build/outputs/bundle/release"
APK_DIR="app/build/outputs/apk/release"

shopt -s nullglob
AABS=("$AAB_DIR"/*.aab)
APKS=("$APK_DIR"/*.apk)

[[ ${#AABS[@]} -gt 0 ]] || fail "No release AAB found in $AAB_DIR"
[[ ${#APKS[@]} -gt 0 ]] || fail "No release APK found in $APK_DIR"

for aab in "${AABS[@]}"; do
  [[ -s "$aab" ]] || fail "AAB is empty: $aab"
  jarsigner -verify -strict "$aab" >/dev/null || fail "AAB signature verification failed: $aab"
  pass "AAB signed and verified: $aab"
done

APKSIGNER=""
if [[ -n "${ANDROID_HOME:-}" ]]; then
  APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name apksigner -type f | sort -V | tail -n 1 || true)
fi

for apk in "${APKS[@]}"; do
  [[ -s "$apk" ]] || fail "APK is empty: $apk"
  if [[ -n "$APKSIGNER" ]]; then
    "$APKSIGNER" verify --verbose "$apk" >/dev/null || fail "APK signature verification failed: $apk"
    pass "APK signed and verified: $apk"
  else
    jarsigner -verify -strict "$apk" >/dev/null || fail "APK JAR signature verification failed: $apk"
    pass "APK signed and JAR-verified: $apk"
  fi
done

SHA_FILE="stickme-release-SHA256SUMS.txt"
{
  for artifact in "${AABS[@]}" "${APKS[@]}"; do
    sha256sum "$artifact"
  done
} > "$SHA_FILE"
pass "SHA256 manifest written: $SHA_FILE"
