#!/usr/bin/env bash
# Builds the reduced ONNX Runtime that the ONNX text recognition provider links against, as the Android AAR and the
# desktop app's Linux x86_64 jar, stages both where `-PlocalOnnxRuntime` builds read them and publish-release.sh uploads
# them from, records their SHA-256 in gradle/verification-metadata.xml, against which Gradle checks the copies it
# downloads from the GitHub release, and writes both apps' licences screen entries for it (write-licences.py).
#
# Microsoft's own packages carry every operator kernel and Microsoft's telemetry: the stock onnxruntime-android AAR
# starts it from a manifest ContentProvider at every app start, and the stock Linux library in
# com.microsoft.onnxruntime:onnxruntime starts it, on by default, when the first model loads. The AAR also brings the
# NNAPI, XNNPACK and WebGPU execution providers. Both builds here keep the CPU execution provider, only the kernels
# listed in required-operators.config, and no telemetry, with the build_params of build-settings.json (less --android
# for Linux).
#
# The Linux jar builds in the container of linux/Dockerfile, so it needs nothing newer than that system's glibc and
# libstdc++. Requires git, Python 3, CMake, Ninja, zip, Docker, a JDK 17 (ONNX Runtime packages the AAR with Android
# Gradle Plugin 7, which fails on newer JDKs; one Gradle provisioned is used when JAVA_HOME is not 17), and the Android
# SDK with the app's NDK version installed. upgrade.sh describes when to run it.
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

# Keep each platform's native build for incremental rebuilds of the same sources, but drop the AAR staging directories:
# they keep the libraries of ABIs that earlier builds listed.
for build in "$work/build" "$work/build-linux"; do
    if [ "$(cat "$build/source-commit" 2>/dev/null)" != "$ORT_COMMIT" ]; then
        rm -rf "$build"
        mkdir -p "$build"
        echo "$ORT_COMMIT" > "$build/source-commit"
    fi
done
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

linux_params=$(python3 -I -c '
import json, sys
print(*(parameter for parameter in json.load(sys.stdin)["build_params"] if parameter != "--android"))' \
    < "$here/build-settings.json")
image=katari-onnxruntime-linux
docker build --quiet --tag "$image" "$here/linux" > /dev/null
mkdir -p "$work/linux-home"
# shellcheck disable=SC2086 # the parameters are separate arguments
docker run --rm --user "$(id -u):$(id -g)" --volume "$work:$work" --volume "$here:$here:ro" \
    --env "HOME=$work/linux-home" --workdir "$work/onnxruntime" "$image" \
    python3 tools/ci_build/build.py --build_dir "$work/build-linux" --config Release \
    --include_ops_by_config "$here/required-operators.config" $linux_params

rm -rf "$(dirname "$staged_aar")"
mkdir -p "$(dirname "$staged_aar")"
built_aar=$work/build/aar_out/Release/com/microsoft/onnxruntime/onnxruntime-android/$ort_version
cp "$built_aar/onnxruntime-android-$ort_version.aar" "$staged_aar"
cp "$work/build-linux/Release/java/build/libs/onnxruntime.jar" "$staged_jar"
python3 -I "$here/record-checksum.py" "$root/gradle/verification-metadata.xml" "$library_version" \
    "$(sha256sum < "$staged_aar" | cut -d' ' -f1)" "$(sha256sum < "$staged_jar" | cut -d' ' -f1)"
python3 -I "$here/write-licences.py" "$work/onnxruntime" "$library_version" "$root"
echo "staged $staged_aar and $staged_jar"
