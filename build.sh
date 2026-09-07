#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-/opt/homebrew/share/android-commandlinetools}"
BT="$SDK/build-tools/36.1.0"
ANDROID_JAR="$SDK/platforms/android-36/android.jar"
BUILD="${BRIDGE_BUILD_DIR:-../../work/chrome-bridge-build}"
OUTPUT_APK="${OUTPUT_APK:-chrome-bitwarden-inline.apk}"
RELEASE_KEYSTORE="${RELEASE_KEYSTORE:-}"
RELEASE_KEY_ALIAS="${RELEASE_KEY_ALIAS:-}"
mkdir -p "$BUILD/deps" "$BUILD/classes" "$BUILD/dex"
BUILD="$(cd "$BUILD" && pwd)"
API="$BUILD/deps/api-101.0.0.aar"
if [[ ! -f "$API" ]]; then
  curl -fsSL https://repo.maven.apache.org/maven2/io/github/libxposed/api/101.0.0/api-101.0.0.aar -o "$API"
fi
unzip -p "$API" classes.jar > "$BUILD/deps/libxposed.jar"
javac --release 17 -cp "$ANDROID_JAR:$BUILD/deps/libxposed.jar" -d "$BUILD/classes" src/org/lyf/chromebitwardeninline/ModuleMain.java
jar cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"$BT/d8" --min-api 30 --lib "$ANDROID_JAR" --classpath "$BUILD/deps/libxposed.jar" --output "$BUILD/dex" "$BUILD/classes.jar"
"$BT/aapt2" link -I "$ANDROID_JAR" --manifest AndroidManifest.xml -o "$BUILD/unsigned.apk"
python3 - "$BUILD" "$PWD/resources" <<'PY'
import pathlib, sys, zipfile
build, resources = map(pathlib.Path, sys.argv[1:])
with zipfile.ZipFile(build/'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(build/'dex/classes.dex', 'classes.dex')
    for p in sorted(resources.rglob('*')):
        if p.is_file(): z.write(p, p.relative_to(resources).as_posix())
PY
"$BT/zipalign" -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
if [[ -z "$RELEASE_KEYSTORE" || -z "$RELEASE_KEY_ALIAS" ]]; then
  echo 'Set RELEASE_KEYSTORE and RELEASE_KEY_ALIAS before building a release.' >&2
  exit 2
fi
if [[ ! -f "$RELEASE_KEYSTORE" ]]; then
  echo "Keystore not found: $RELEASE_KEYSTORE" >&2
  exit 2
fi

SIGN_ARGS=(--ks "$RELEASE_KEYSTORE" --ks-key-alias "$RELEASE_KEY_ALIAS" --out "$OUTPUT_APK")
if [[ -n "${RELEASE_STORE_PASSWORD:-}" ]]; then
  SIGN_ARGS+=(--ks-pass env:RELEASE_STORE_PASSWORD)
fi
if [[ -n "${RELEASE_KEY_PASSWORD:-}" ]]; then
  SIGN_ARGS+=(--key-pass env:RELEASE_KEY_PASSWORD)
fi
"$BT/apksigner" sign "${SIGN_ARGS[@]}" "$BUILD/aligned.apk"
"$BT/apksigner" verify --verbose "$OUTPUT_APK"
echo "Built $OUTPUT_APK"
