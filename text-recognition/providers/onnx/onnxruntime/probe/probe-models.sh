#!/usr/bin/env bash
# Runs every model of the ONNX text recognition provider on a connected device with the ONNX Runtime of an AAR.
#
#   probe-models.sh <onnxruntime.aar> <abi> <adb-serial> <models-dir> <output-dir>
#
# For each model it prints whether it loaded and checksums of its outputs on deterministic inputs, and pulls the graph
# as ORT_ENABLE_ALL optimized it for that device into <output-dir>. Compare the printed checksums of two AARs to check
# that a reduced build computes what the full one does; feed the optimized graphs to
# create_reduced_build_config.py to find the kernels a reduced build needs (see ../build-aar.sh).
#
# <models-dir> holds the provider's models (OnnxModelArtifacts): detector-v4-s_int8.onnx, encoder_model_int8.onnx,
# decoder_model_int8.onnx, the PaddleOCR det.onnx, and one rec*.onnx per PaddleOCR script.
set -euo pipefail

aar=$(realpath "$1") abi=$2 serial=$3 models=$(realpath "$4") output=$(realpath -m "$5")
root=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' "$root/local.properties")}
ndk_version=$(sed -n 's/^android-ndk = "\(.*\)"/\1/p' "$root/gradle/mihon.versions.toml")
toolchain=$sdk/ndk/$ndk_version/toolchains/llvm/prebuilt/linux-x86_64/bin
case $abi in
    arm64-v8a) target=aarch64-linux-android29 ;;
    armeabi-v7a) target=armv7a-linux-androideabi29 ;;
    x86_64) target=x86_64-linux-android29 ;;
    *) echo "unknown ABI $abi" >&2; exit 2 ;;
esac

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -q "$aar" "headers/*" "jni/$abi/libonnxruntime.so" -d "$work"
"$toolchain/$target-clang" -O2 -Wall -Werror -I "$work/headers" "$(dirname "$0")/probe.c" \
    -L "$work/jni/$abi" -lonnxruntime -lm -o "$work/probe"

device=/data/local/tmp/katari-onnxruntime-probe
adb -s "$serial" shell "rm -rf $device/runtime $device/optimized && mkdir -p $device/models $device/runtime $device/optimized"
adb -s "$serial" push -q "$work/probe" "$work/jni/$abi/libonnxruntime.so" "$device/runtime/" >/dev/null
mkdir -p "$output"
for model in "$models"/*.onnx; do
    name=$(basename "$model")
    case $name in
        *decoder*) inputs="i=2:1,8 f:1,197,768" ;;
        *detector*) inputs="f:1,3,640,640 i=640:1,2" ;;
        *encoder*) inputs="f:1,3,224,224" ;;
        det*) inputs="f:1,3,640,640" ;;
        rec*) inputs="f:1,3,48,320" ;;
        *) echo "no inputs known for $name" >&2; exit 2 ;;
    esac
    adb -s "$serial" shell "[ -f $device/models/$name ]" || adb -s "$serial" push -q "$model" "$device/models/" >/dev/null
    echo "== $name"
    adb -s "$serial" shell "cd $device/runtime && LD_LIBRARY_PATH=. ./probe $device/models/$name \
        $device/optimized/$name $inputs"
    adb -s "$serial" pull -q "$device/optimized/$name" "$output/$name" >/dev/null
done
