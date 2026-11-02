#!/usr/bin/env bash
# Runs probe-models.sh with an AAR on every ABI that build-settings.json builds, each on a connected device whose
# primary ABI family matches: arm64-v8a and armeabi-v7a on an arm64 device, x86_64 on an emulator. An emulator's
# translated arm ABIs never count: they would probe the translator, not the CPU the app runs on.
#
#   probe-abis.sh <onnxruntime.aar> <models-dir> <output-dir>
#
# Writes <output-dir>/<abi>.log with the probe's report and <output-dir>/<abi>/ with the optimized graphs, and fails if
# a model does not load or run.
set -euo pipefail

aar=$(realpath "$1") models=$(realpath "$2") output=$(realpath -m "$3")
here=$(cd "$(dirname "$0")" && pwd)

device_for() {
    local abi=$1 primary serial
    case $abi in
        arm64-v8a | armeabi-v7a) primary=arm64-v8a ;;
        x86_64) primary=x86_64 ;;
    esac
    for serial in $(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }'); do
        if [ "$(adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')" = "$primary" ] &&
            adb -s "$serial" shell getprop ro.product.cpu.abilist | tr -d '\r' | tr ',' '\n' | grep -qx "$abi"; then
            echo "$serial"
            return
        fi
    done
    return 1
}

abis=$(python3 -I -c 'import json, sys; print(*json.load(sys.stdin)["build_abis"])' < "$here/../build-settings.json")
declare -A serials
for abi in $abis; do
    serials[$abi]=$(device_for "$abi") || { echo "no connected device runs $abi code natively" >&2; exit 1; }
done

rm -rf "$output"
mkdir -p "$output"
for abi in $abis; do
    echo "probing $abi on ${serials[$abi]}"
    "$here/probe-models.sh" "$aar" "$abi" "${serials[$abi]}" "$models" "$output/$abi" > "$output/$abi.log" || true
    loaded=$(grep -c '^LOADED ' "$output/$abi.log" || true)
    expected=$(find "$models" -name '*.onnx' | wc -l)
    if grep -q '^ERROR ' "$output/$abi.log" || [ "$loaded" -ne "$expected" ]; then
        grep -B1 '^ERROR ' "$output/$abi.log" >&2 || true
        echo "$abi: $loaded of $expected models loaded; see $output/$abi.log" >&2
        exit 1
    fi
done
