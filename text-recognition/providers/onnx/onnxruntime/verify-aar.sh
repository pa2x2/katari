#!/usr/bin/env bash
# Checks the staged AAR before it is published: it must declare no ContentProvider (the stock AAR's telemetry starts
# from one), load every model of the provider on each ABI, and compute the same output checksums as the stock AAR of the
# same ONNX Runtime version. Needs the devices probe/probe-abis.sh does.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

if [ ! -f "$staged_aar" ]; then
    echo "no AAR staged at $staged_aar; run build-aar.sh" >&2
    exit 1
fi
if unzip -p "$staged_aar" AndroidManifest.xml | grep -q '<provider'; then
    echo "$staged_aar declares a ContentProvider" >&2
    exit 1
fi

probe_stock
reduced=$work/probe/reduced-$aar_version
"$here/probe/probe-abis.sh" "$staged_aar" "$models" "$reduced"

status=0
for log in "$stock_probe"/*.log; do
    abi=$(basename "$log" .log)
    if diff -u --label "stock $abi" --label "reduced $abi" \
        <(grep -E '^(== |LOADED |OUTPUT )' "$log") <(grep -E '^(== |LOADED |OUTPUT )' "$reduced/$abi.log"); then
        echo "$abi: outputs match the stock AAR"
    else
        status=1
    fi
done
exit $status
