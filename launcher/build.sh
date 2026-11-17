#!/usr/bin/env bash
# Minimal, Gradle-free APK build for the RooTISA launcher.
# Tools: aapt + javac + dx + zipalign + apksigner (no Android Gradle Plugin, no AndroidX).
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"

# --- Toolchain locations (override via env if needed) ---
TOOLS="${ANDROID_TOOLS:-$HOME/android-tools}"
ANDROID_JAR="${ANDROID_JAR:-$TOOLS/android.jar}"
DX_JAR="${DX_JAR:-$TOOLS/dx.jar}"
AAPT="${AAPT:-aapt}"
ZIPALIGN="${ZIPALIGN:-zipalign}"
APKSIGNER="${APKSIGNER:-apksigner}"

OUT="$HERE/build"
GEN="$OUT/gen"
CLASSES="$OUT/classes"
KS="$OUT/debug.keystore"
APP="RooTISA"

echo "==> clean"
rm -rf "$OUT"; mkdir -p "$GEN" "$CLASSES"

echo "==> [1/6] aapt: compile resources + generate R.java + base apk"
"$AAPT" package -f -m \
    -J "$GEN" \
    -M AndroidManifest.xml \
    -S res \
    -I "$ANDROID_JAR" \
    -F "$OUT/base.apk"

echo "==> [2/6] ecj: compile sources to Java 6 bytecode (dx 1.7 reads up to major 50)"
find src "$GEN" -name '*.java' > "$OUT/sources.txt"
ecj -1.6 -encoding UTF-8 -nowarn -proc:none \
    -bootclasspath "$ANDROID_JAR" \
    -classpath "$ANDROID_JAR" \
    -d "$CLASSES" \
    @"$OUT/sources.txt"

echo "==> [3/6] dx: dex the classes -> classes.dex"
java -cp "$DX_JAR" com.android.dx.command.Main --dex \
    --output="$OUT/classes.dex" "$CLASSES"

echo "==> [4/6] package: add classes.dex into the apk"
cp "$OUT/base.apk" "$OUT/$APP.unaligned.apk"
( cd "$OUT" && "$AAPT" add "$APP.unaligned.apk" classes.dex >/dev/null )

echo "==> [5/6] zipalign"
"$ZIPALIGN" -f 4 "$OUT/$APP.unaligned.apk" "$OUT/$APP.aligned.apk"

echo "==> [6/6] sign (generate debug key if missing, v1+v2 for old devices)"
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=RooTISA Debug, O=RooTISA, C=RU" >/dev/null 2>&1
fi
"$APKSIGNER" sign \
    --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --min-sdk-version 16 \
    --v1-signing-enabled true --v2-signing-enabled true \
    --out "$OUT/$APP.apk" "$OUT/$APP.aligned.apk"

"$APKSIGNER" verify --print-certs "$OUT/$APP.apk" >/dev/null && echo "signature OK"
echo
echo "==> BUILT: $OUT/$APP.apk  ($(du -h "$OUT/$APP.apk" | cut -f1))"
