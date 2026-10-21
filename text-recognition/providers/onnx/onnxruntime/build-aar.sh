#!/usr/bin/env bash
# Builds the reduced ONNX Runtime that the ONNX text recognition provider links against, stages it where
# `-PlocalOnnxRuntime` builds read it and publish-release.sh uploads it from, and records its SHA-256 in
# gradle/verification-metadata.xml, against which Gradle checks the copy it downloads from the GitHub release.
#
# The stock com.microsoft.onnxruntime:onnxruntime-android AAR carries every operator kernel, the NNAPI, XNNPACK and
# WebGPU execution providers, and Microsoft's telemetry, whose manifest ContentProvider loads the library at every app
# start. This build keeps the CPU execution provider, only the kernels listed in required-operators.config, and no
# telemetry.
#
# Requires git, Python 3, CMake, Ninja, zip, a JDK 17 (ONNX Runtime packages the AAR with Android Gradle Plugin 7, which
# fails on newer JDKs; one Gradle provisioned is used when JAVA_HOME is not 17), and the Android SDK with the app's NDK
# version installed. upgrade.sh describes when to run it.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

java_major() {
    "$1/bin/java" -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p'
}
if [ -z "${JAVA_HOME:-}" ] || [ "$(java_major "$JAVA_HOME")" != 17 ]; then
    JAVA_HOME=$(find "$HOME/.gradle/jdks" -mindepth 1 -maxdepth 1 -type d -name '*-17-*' 2>/dev/null | sort | tail -n 1)
    if [ -z "$JAVA_HOME" ]; then
        echo "set JAVA_HOME to a JDK 17" >&2
        exit 1
    fi
fi
export JAVA_HOME PATH=$JAVA_HOME/bin:$PATH

prepare_source

# Keep the per-ABI native builds for incremental rebuilds of the same sources, but drop the AAR staging directories:
# they keep the libraries of ABIs that earlier builds listed.
if [ "$(cat "$work/build/source-commit" 2>/dev/null)" != "$ORT_COMMIT" ]; then
    rm -rf "$work/build"
fi
rm -rf "$work/build/aar_out" "$work/build/intermediates/aar" "$work/build/intermediates/jnilibs" \
    "$work/build/intermediates/executables"
(
    cd "$work/onnxruntime"
    "$python" tools/ci_build/github/android/build_aar_package.py \
        --android_sdk_path "$sdk" \
        --android_ndk_path "$sdk/ndk/$ndk_version" \
        --build_dir "$work/build" \
        --include_ops_by_config "$here/required-operators.config" \
        --config Release \
        "$here/build-settings.json"
)
echo "$ORT_COMMIT" > "$work/build/source-commit"

rm -rf "$(dirname "$staged_aar")"
mkdir -p "$(dirname "$staged_aar")"
built=$work/build/aar_out/Release/com/microsoft/onnxruntime/onnxruntime-android/$ort_version
cp "$built/onnxruntime-android-$ort_version.aar" "$staged_aar"
python3 -I "$here/record-checksum.py" "$root/gradle/verification-metadata.xml" "$aar_version" \
    "$(sha256sum < "$staged_aar" | cut -d' ' -f1)"
echo "staged $staged_aar"
