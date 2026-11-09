#!/usr/bin/env bash
# Runs every model of the ONNX text recognition provider on this machine's x86_64 CPU with the Linux ONNX Runtime of a
# jar, as probe-models.sh does on an Android device, for ../verify.sh and ../required-operators.sh.
#
#   probe-linux.sh <onnxruntime.jar> <C API headers dir> <models-dir> <output-dir>
#
# Writes <output-dir>/linux-x64.log with the probe's report and <output-dir>/linux-x64/ with the graphs as
# ORT_ENABLE_ALL optimized them for this CPU, and fails if a model does not load or run. The probe runs with
# ORT_DISABLE_TELEMETRY set, since Microsoft's own jar would send telemetry otherwise.
set -euo pipefail

jar=$(realpath "$1") headers=$(realpath "$2") models=$(realpath "$3") output=$(realpath -m "$4")
here=$(cd "$(dirname "$0")" && pwd)
. "$here/model-inputs.sh"

scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
unzip -q "$jar" ai/onnxruntime/native/linux-x64/libonnxruntime.so -d "$scratch"
runtime=$scratch/ai/onnxruntime/native/linux-x64
soname=$(readelf -d "$runtime/libonnxruntime.so" | sed -n 's/.*(SONAME).*\[\(.*\)\]/\1/p')
if [ -n "$soname" ] && [ "$soname" != libonnxruntime.so ]; then
    ln -s libonnxruntime.so "$runtime/$soname"
fi
cc -O2 -Wall -Werror -I "$headers" "$here/probe.c" -L "$runtime" -lonnxruntime -lm -o "$runtime/probe"

log=$output/linux-x64.log
graphs=$output/linux-x64
rm -rf "$log" "$graphs"
mkdir -p "$graphs"
echo "probing linux-x64 on this machine"
for model in "$models"/*.onnx; do
    name=$(basename "$model")
    inputs=$(model_inputs "$name")
    echo "== $name"
    # shellcheck disable=SC2086 # the inputs are separate arguments
    ORT_DISABLE_TELEMETRY=1 LD_LIBRARY_PATH=$runtime "$runtime/probe" "$model" "$graphs/$name" $inputs || true
done > "$log"

loaded=$(grep -c '^LOADED ' "$log" || true)
expected=$(find "$models" -name '*.onnx' | wc -l)
if grep -q '^ERROR ' "$log" || [ "$loaded" -ne "$expected" ]; then
    grep -B1 '^ERROR ' "$log" >&2 || true
    echo "linux-x64: $loaded of $expected models loaded; see $log" >&2
    exit 1
fi
