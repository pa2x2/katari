#!/usr/bin/env bash
# Builds the reduced ONNX Runtime that the ONNX text recognition provider links against and installs it into the Maven
# repository next to this script, from which Gradle resolves `app.katari.onnxruntime:onnxruntime-android`.
#
# The stock com.microsoft.onnxruntime:onnxruntime-android AAR carries every operator kernel, the NNAPI, XNNPACK and
# WebGPU execution providers, and Microsoft's telemetry, whose manifest ContentProvider loads the library at every app
# start. This build keeps the CPU execution provider, only the kernels listed in required-operators.config, and no
# telemetry.
#
# Requires git, Python 3, CMake, Ninja, zip, JDK 17 as JAVA_HOME (ONNX Runtime packages the AAR with Android Gradle
# Plugin 7, which fails on newer JDKs), and the Android SDK with the app's NDK version installed.
#
# After adding or changing a model, update required-operators.config before rebuilding:
#   1. Download the provider's models (OnnxModelArtifacts) into one directory.
#   2. For each of arm64-v8a, armeabi-v7a (an arm64 device that runs 32-bit code serves both) and x86_64 (an
#      emulator), run
#      probe/probe-models.sh <stock onnxruntime-android AAR> <abi> <adb-serial> <models> <graphs>/<abi>
#   3. Put the models and all optimized graphs into one directory, run the ONNX Runtime checkout's
#      tools/python/create_reduced_build_config.py --format ONNX <directory> <config> and replace the operator lines of
#      required-operators.config with the result.
#   4. Rebuild, then run probe-models.sh with the new AAR on each ABI: every model must load and its output checksums
#      must match those of the stock AAR.
set -euo pipefail

ORT_VERSION=1.30.0
ORT_COMMIT=f2c39fe2f838cf35ce7da92824f5a5e3ee6e88a7

here=$(cd "$(dirname "$0")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' "$root/local.properties")}
ndk_version=$(sed -n 's/^android-ndk = "\(.*\)"/\1/p' "$root/gradle/mihon.versions.toml")
work=${ONNXRUNTIME_BUILD_DIR:-$HOME/.cache/katari-onnxruntime}

mkdir -p "$work"
if [ ! -d "$work/onnxruntime" ]; then
    git clone --depth 1 --branch "v$ORT_VERSION" https://github.com/microsoft/onnxruntime.git "$work/onnxruntime"
fi
if [ "$(git -C "$work/onnxruntime" rev-parse HEAD)" != "$ORT_COMMIT" ]; then
    echo "$work/onnxruntime is not ONNX Runtime $ORT_VERSION ($ORT_COMMIT)" >&2
    exit 1
fi
if [ ! -x "$work/venv/bin/python" ]; then
    python3 -m venv "$work/venv"
    "$work/venv/bin/pip" install --quiet flatbuffers==25.12.19
fi

# Keep the per-ABI native builds for incremental rebuilds, but drop the AAR staging directories: they keep the
# libraries of ABIs that earlier builds listed.
rm -rf "$work/build/aar_out" "$work/build/intermediates/aar" "$work/build/intermediates/jnilibs" \
    "$work/build/intermediates/executables"
(
    cd "$work/onnxruntime"
    "$work/venv/bin/python" tools/ci_build/github/android/build_aar_package.py \
        --android_sdk_path "$sdk" \
        --android_ndk_path "$sdk/ndk/$ndk_version" \
        --build_dir "$work/build" \
        --include_ops_by_config "$here/required-operators.config" \
        --config Release \
        "$here/build-settings.json"
)

repository=$here/maven/app/katari/onnxruntime/onnxruntime-android/$ORT_VERSION
rm -rf "$here/maven"
mkdir -p "$repository"
cp "$work/build/aar_out/Release/com/microsoft/onnxruntime/onnxruntime-android/$ORT_VERSION/onnxruntime-android-$ORT_VERSION.aar" \
    "$repository/"
cat > "$repository/onnxruntime-android-$ORT_VERSION.pom" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>app.katari.onnxruntime</groupId>
  <artifactId>onnxruntime-android</artifactId>
  <version>$ORT_VERSION</version>
  <packaging>aar</packaging>
  <name>ONNX Runtime</name>
  <description>ONNX Runtime $ORT_VERSION for Android, reduced to the CPU execution provider and the operators of Katari's text recognition models, without telemetry.</description>
  <url>https://onnxruntime.ai/</url>
  <organization>
    <name>Microsoft</name>
    <url>https://www.microsoft.com</url>
  </organization>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>https://opensource.org/licenses/MIT</url>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/microsoft/onnxruntime</url>
  </scm>
</project>
EOF
