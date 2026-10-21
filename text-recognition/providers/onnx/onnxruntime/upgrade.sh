#!/usr/bin/env bash
# Moves the reduced ONNX Runtime to another ONNX Runtime version, or, without one, rebuilds the current version after
# models were added or changed.
#
#   upgrade.sh [version]
#
# Points gradle/libs.versions.toml at the new AAR version (and workspace.sh at the version's tag), then runs
# required-operators.sh, build-aar.sh and verify-aar.sh, stopping at the first failure. Needs an arm64 device that runs
# 32-bit code and an x86_64 emulator connected. After a failure, fix its cause and run the remaining scripts directly,
# as running this again would move to yet another version. Afterwards, try the app with -PlocalOnnxRuntime, run
# publish-release.sh, commit, and push.
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
catalog=$root/gradle/libs.versions.toml
current=$(sed -n 's/^onnxruntime = "\([^"]*\)".*/\1/p' "$catalog")

if [ $# -eq 1 ]; then
    version=$1
    commit=$(git ls-remote --tags https://github.com/microsoft/onnxruntime.git "v$version" "v$version^{}" |
        tail -n 1 | cut -f1)
    if [ -z "$commit" ]; then
        echo "ONNX Runtime has no tag v$version" >&2
        exit 1
    fi
    sed -i "s/^ORT_COMMIT=.*/ORT_COMMIT=$commit/" "$here/workspace.sh"
else
    base=${current%%-*}
    build=${current#"$base"}
    build=${build#-}
    version=$base-$((${build:-1} + 1))
fi
sed -i "s/^onnxruntime = \"$current\"/onnxruntime = \"$version\"/" "$catalog"
echo "onnxruntime $current -> $version"

"$here/required-operators.sh"
"$here/build-aar.sh"
"$here/verify-aar.sh"
