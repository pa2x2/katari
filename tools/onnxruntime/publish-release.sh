#!/usr/bin/env bash
# Publishes the staged AAR and Linux jar together as the GitHub release Gradle downloads them from (see
# settings.gradle.kts), after checking that they are the files gradle/verification-metadata.xml pins. Push the commit
# that pins them only once the release exists.
#
# The release is never marked latest, so the website's download link and GitHub's latest release stay on the app, and
# its tag does not start with "v", so the app's update check and the release-triggered workflows ignore it. Never delete
# one: the commits that pin its libraries can no longer be built without it.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

repository=$(git -C "$root" remote get-url origin | sed -E 's#^(git@github\.com:|https://github\.com/)##; s#\.git$##')

pinned() {
    grep -A2 "<artifact name=\"$(basename "$1")\">" "$root/gradle/verification-metadata.xml" |
        sed -n 's/.*<sha256 value="\([0-9a-f]*\)".*/\1/p'
}

checksums=()
for staged in "$staged_aar" "$staged_jar"; do
    digest=$(pinned "$staged")
    if [ ! -f "$staged" ] || [ -z "$digest" ] || [ "$(sha256sum < "$staged" | cut -d' ' -f1)" != "$digest" ]; then
        echo "$staged is missing or not the file gradle/verification-metadata.xml pins; run build.sh" >&2
        exit 1
    fi
    checksums+=("- \`$(basename "$staged")\`: \`$digest\`")
done
if gh release view "$release_tag" --repo "$repository" > /dev/null 2>&1; then
    echo "$repository already has a release $release_tag" >&2
    exit 1
fi

gh release create "$release_tag" "$staged_aar" "$staged_jar" --repo "$repository" --latest=false \
    --title "ONNX Runtime $library_version for Katari (build dependency)" \
    --notes "Not an app release. ONNX Runtime $ort_version ($ORT_COMMIT) reduced to the CPU execution provider and \
the kernels of Katari's text recognition models, without telemetry, for Android (the AAR) and for the desktop app on \
x86_64 Linux (the jar), built by \`tools/onnxruntime/build.sh\`. Gradle downloads them from here and checks them \
against \`gradle/verification-metadata.xml\`.

SHA-256:
$(printf '%s\n' "${checksums[@]}")"

for staged in "$staged_aar" "$staged_jar"; do
    asset=$(basename "$staged")
    download=https://github.com/$repository/releases/download/$release_tag/$asset
    downloaded=$(curl -sSfL "$download" | sha256sum | cut -d' ' -f1)
    if [ "$downloaded" != "$(pinned "$staged")" ]; then
        echo "the published $asset has SHA-256 $downloaded instead of $(pinned "$staged")" >&2
        exit 1
    fi
done
echo "published https://github.com/$repository/releases/tag/$release_tag"
