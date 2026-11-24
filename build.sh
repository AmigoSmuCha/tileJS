#!/usr/bin/env bash
# Builds build/tileJS.apk with the plain Android SDK tools: no Gradle, nothing
# to download. GitHub runs this for every push (see .github/workflows/build.yml).
#
# To build on your own computer you need the Android SDK (ANDROID_HOME set,
# with build-tools and a platform installed), a JDK 17+ and zip.
set -euo pipefail
cd "$(dirname "$0")"

MIN_SDK=26       # Android 8.0
TARGET_SDK=34
VERSION_CODE="${VERSION_CODE:-1}"
VERSION_NAME="${VERSION_NAME:-dev}"
KEY_PASS="tilejs-release"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] || [ ! -d "$SDK/build-tools" ]; then
    echo "Android SDK not found - set ANDROID_HOME" >&2
    exit 1
fi

# Prefer a known build-tools version, otherwise take the newest installed
TOOLS=""
for version in 35.0.0 34.0.0 36.0.0; do
    if [ -x "$SDK/build-tools/$version/aapt2" ]; then
        TOOLS="$SDK/build-tools/$version"
        break
    fi
done
[ -n "$TOOLS" ] || TOOLS="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -n 1)"

PLATFORM="$SDK/platforms/android-$TARGET_SDK"
if [ ! -f "$PLATFORM/android.jar" ]; then
    PLATFORM="$SDK/platforms/$(ls "$SDK/platforms" | grep -E '^android-[0-9]+$' | sort -V | tail -n 1)"
fi
ANDROID_JAR="$PLATFORM/android.jar"
[ -f "$ANDROID_JAR" ] || { echo "No Android platform in $SDK/platforms" >&2; exit 1; }

echo "build-tools: $TOOLS"
echo "platform:    $PLATFORM"
echo "version:     $VERSION_NAME ($VERSION_CODE)"

rm -rf build
mkdir -p build/classes build/dex build/gen

echo "== resources"
"$TOOLS/aapt2" compile --dir app/res -o build/res.zip
"$TOOLS/aapt2" link -o build/unsigned.apk -I "$ANDROID_JAR" \
    --manifest app/AndroidManifest.xml \
    --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
    --java build/gen \
    build/res.zip

echo "== libraries (Shizuku)"
SHIZUKU_VERSION=13.1.5
mkdir -p libs
for lib in api aidl provider shared; do
    if [ ! -f "libs/shizuku-$lib.jar" ]; then
        curl -fsSL --retry 3 -o "build/$lib.aar" \
            "https://repo1.maven.org/maven2/dev/rikka/shizuku/$lib/$SHIZUKU_VERSION/$lib-$SHIZUKU_VERSION.aar"
        unzip -p "build/$lib.aar" classes.jar > "libs/shizuku-$lib.jar"
    fi
done

echo "== code"
# Java 8 language level; the Android classes come from android.jar
javac --release 8 -Xlint:-options -proc:none -encoding UTF-8 \
    -classpath "$ANDROID_JAR" -d build/classes $(find app/src build/gen -name '*.java')
jar cf build/classes.jar -C build/classes .
"$TOOLS/d8" --release --min-api "$MIN_SDK" --lib "$ANDROID_JAR" --output build/dex \
    build/classes.jar libs/shizuku-*.jar

echo "== package and sign"
zip -q -j build/unsigned.apk build/dex/classes.dex
"$TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$TOOLS/apksigner" sign --ks app/release.keystore --ks-key-alias tilejs \
    --ks-pass "pass:$KEY_PASS" --key-pass "pass:$KEY_PASS" \
    --out build/tileJS.apk build/aligned.apk
"$TOOLS/apksigner" verify build/tileJS.apk

# Android 11+ refuses APKs whose resource table is compressed - fail here, not on the phone
listing="$(unzip -v build/tileJS.apk resources.arsc)"
case "$listing" in
    *Stored*) ;;
    *) echo "resources.arsc is compressed - the APK would not install" >&2; exit 1 ;;
esac

ls -l build/tileJS.apk
