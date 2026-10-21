#!/usr/bin/env bash
# Publishes the staged AAR as the GitHub release Gradle downloads it from (see settings.gradle.kts), after checking that
# it is the AAR gradle/verification-metadata.xml pins. Push the commit that pins it only once the release exists.
#
# The release is never marked latest, so the website's download link and GitHub's latest release stay on the app, and
# its tag does not start with "v", so the app's update check ignores it. Never delete one: the commits that pin its AAR
# can no longer be built without it.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

repository=$(git -C "$root" remote get-url origin | sed -E 's#^(git@github\.com:|https://github\.com/)##; s#\.git$##')
asset=$(basename "$staged_aar")
pinned=$(grep -A2 "<artifact name=\"$asset\">" "$root/gradle/verification-metadata.xml" |
    sed -n 's/.*<sha256 value="\([0-9a-f]*\)".*/\1/p')
if [ ! -f "$staged_aar" ] || [ -z "$pinned" ] || [ "$(sha256sum < "$staged_aar" | cut -d' ' -f1)" != "$pinned" ]; then
    echo "$staged_aar is missing or not the AAR gradle/verification-metadata.xml pins; run build-aar.sh" >&2
    exit 1
fi
if gh release view "$release_tag" --repo "$repository" > /dev/null 2>&1; then
    echo "$repository already has a release $release_tag" >&2
    exit 1
fi

gh release create "$release_tag" "$staged_aar" --repo "$repository" --latest=false \
    --title "ONNX Runtime $aar_version for Katari (build dependency)" \
    --notes "Not an app release. ONNX Runtime $ort_version ($ORT_COMMIT) reduced to the CPU execution provider and \
the kernels of Katari's text recognition models, without telemetry, built by \
\`text-recognition/providers/onnx/onnxruntime/build-aar.sh\`. Gradle downloads it from here and checks it against \
\`gradle/verification-metadata.xml\`.

SHA-256: \`$pinned\`"

download=https://github.com/$repository/releases/download/$release_tag/$asset
downloaded=$(curl -sSfL "$download" | sha256sum | cut -d' ' -f1)
if [ "$downloaded" != "$pinned" ]; then
    echo "the published $asset has SHA-256 $downloaded instead of $pinned" >&2
    exit 1
fi
echo "published https://github.com/$repository/releases/tag/$release_tag"
