#!/usr/bin/env bash
# Checks the staged libraries before they are published. Neither may carry telemetry: the stock AAR's starts from a
# manifest ContentProvider, and builds with telemetry link Microsoft's 1DS client, which carries its collector's
# address. The Linux jar must also load on the distributions the desktop app supports (check-linux-library.py). Both
# must load every model of the provider, the AAR on each ABI and the jar on this machine, and compute the same output
# checksums as Microsoft's stock packages of the same ONNX Runtime version. Needs the devices probe/probe-abis.sh does.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

for staged in "$staged_aar" "$staged_jar"; do
    if [ ! -f "$staged" ]; then
        echo "nothing staged at $staged; run build.sh" >&2
        exit 1
    fi
done
if unzip -p "$staged_aar" AndroidManifest.xml | grep -q '<provider'; then
    echo "$staged_aar declares a ContentProvider" >&2
    exit 1
fi
for library in $(unzip -Z1 "$staged_aar" 'jni/*/libonnxruntime.so'); do
    if unzip -p "$staged_aar" "$library" | grep -a -q 'events\.data\.microsoft\.com'; then
        echo "$library in $staged_aar contains telemetry" >&2
        exit 1
    fi
done
python3 -I "$here/check-linux-library.py" "$staged_jar"

probe_stock
reduced=$work/probe/reduced-$library_version
probe_libraries "$staged_aar" "$staged_jar" "$reduced"

status=0
for log in "$stock_probe"/*.log; do
    platform=$(basename "$log" .log)
    if diff -u --label "stock $platform" --label "reduced $platform" \
        <(grep -E '^(== |LOADED |OUTPUT )' "$log") <(grep -E '^(== |LOADED |OUTPUT )' "$reduced/$platform.log"); then
        echo "$platform: outputs match the stock library"
    else
        status=1
    fi
done
exit $status
