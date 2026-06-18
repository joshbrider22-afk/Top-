#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
cd "$ROOT"

fail() { echo "❌ $1" >&2; exit 1; }
pass() { echo "✅ $1"; }
warn() { echo "⚠️ $1"; }

[[ -f settings.gradle.kts ]] && pass "settings.gradle.kts found" || fail "settings.gradle.kts missing"
[[ -f build.gradle.kts ]] && pass "root build.gradle.kts found" || fail "root build.gradle.kts missing"
[[ -f app/build.gradle.kts ]] && pass "app build.gradle.kts found" || fail "app/build.gradle.kts missing"
[[ -f app/src/main/AndroidManifest.xml ]] && pass "AndroidManifest.xml found" || fail "AndroidManifest.xml missing"
[[ -f app/src/main/res/xml/method.xml ]] && pass "IME method.xml found" || fail "Keyboard IME method.xml missing"
[[ -f app/src/main/res/xml/file_paths.xml ]] && pass "FileProvider paths found" || fail "FileProvider paths missing"
[[ -f app/proguard-rules.pro ]] && pass "release shrink rules found" || fail "app/proguard-rules.pro missing"

grep -q 'applicationId = "com.stickme.app"' app/build.gradle.kts && pass "applicationId locked: com.stickme.app" || fail "applicationId is not com.stickme.app"
grep -q 'namespace = "com.stickme.app"' app/build.gradle.kts && pass "namespace locked: com.stickme.app" || fail "namespace is not com.stickme.app"
grep -q 'BIND_INPUT_METHOD' app/src/main/AndroidManifest.xml && pass "keyboard permission declared" || fail "BIND_INPUT_METHOD permission missing"
grep -q 'android.view.InputMethod' app/src/main/AndroidManifest.xml && pass "keyboard service intent declared" || fail "InputMethod service intent missing"
grep -q 'FileProvider' app/src/main/AndroidManifest.xml && pass "FileProvider declared" || fail "FileProvider missing"

if [[ -f keystore.properties ]]; then
  pass "keystore.properties present"
  grep -q '^storeFile=' keystore.properties || fail "keystore.properties missing storeFile"
  grep -q '^storePassword=' keystore.properties || fail "keystore.properties missing storePassword"
  grep -q '^keyAlias=' keystore.properties || fail "keystore.properties missing keyAlias"
  grep -q '^keyPassword=' keystore.properties || fail "keystore.properties missing keyPassword"
else
  warn "keystore.properties missing — signed release cannot be produced until signing secrets are restored"
fi

if [[ -d secrets ]]; then
  warn "secrets directory exists locally; confirm it is ignored and never committed"
fi

echo "StickMe release preflight complete."
