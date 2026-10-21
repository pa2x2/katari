#!/usr/bin/env bash
# Regenerates the operator lines of required-operators.config: every kernel the provider's models use as published and
# as ORT_ENABLE_ALL optimizes them with the stock AAR of this version on each ABI's CPU, which adds fused com.microsoft
# kernels and, on x86_64, the NCHWc layout kernels. Needs the devices probe/probe-abis.sh does.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

prepare_source
probe_stock

inputs=$work/operators/inputs
rm -rf "$work/operators"
mkdir -p "$inputs"
cp "$models"/*.onnx "$inputs/"
for graphs in "$stock_probe"/*/; do
    abi=$(basename "$graphs")
    for graph in "$graphs"*.onnx; do
        cp "$graph" "$inputs/$abi-$(basename "$graph")"
    done
done
"$python" "$work/onnxruntime/tools/python/create_reduced_build_config.py" --format ONNX "$inputs" \
    "$work/operators/required-operators.config"

config=$here/required-operators.config
{
    grep '^#' "$config"
    grep -v -e '^#' -e '^$' "$work/operators/required-operators.config"
} > "$work/operators/merged.config"
mv "$work/operators/merged.config" "$config"
