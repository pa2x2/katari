#!/usr/bin/env bash
# Publishes the staged jar as the GitHub release Gradle downloads it from (see settings.gradle.kts), after checking that
# it is the jar gradle/verification-metadata.xml pins. Push the commit that pins it only once the release exists.
#
# The release is never marked latest, so the website's download link and GitHub's latest release stay on the app, and
# its tag starts with neither "v" nor "desktop-v", so the apps' update checks and the release-triggered workflows ignore
# it. Never delete one: the commits that pin its jar can no longer be built without it.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

repository=$(git -C "$root" remote get-url origin | sed -E 's#^(git@github\.com:|https://github\.com/)##; s#\.git$##')
asset=$(basename "$staged_jar")
pinned=$(grep -A2 "<artifact name=\"$asset\">" "$root/gradle/verification-metadata.xml" |
    sed -n 's/.*<sha256 value="\([0-9a-f]*\)".*/\1/p')
if [ ! -f "$staged_jar" ] || [ -z "$pinned" ] || [ "$(sha256sum < "$staged_jar" | cut -d' ' -f1)" != "$pinned" ]; then
    echo "$staged_jar is missing or not the jar gradle/verification-metadata.xml pins; run build-jar.sh" >&2
    exit 1
fi
if gh release view "$release_tag" --repo "$repository" > /dev/null 2>&1; then
    echo "$repository already has a release $release_tag" >&2
    exit 1
fi

components=$(for entry in "${SOURCES[@]}"; do
    read -r -a fields <<< "$entry"
    echo "- ${fields[0]} ${fields[1]}: ${fields[2]} (SHA-256 \`${fields[3]}\`)"
done)
gh release create "$release_tag" "$staged_jar" --repo "$repository" --latest=false \
    --title "libarchive $jar_version for Katari's desktop app (build dependency)" \
    --notes "Not an app release. libarchive for x86_64 Linux with its compression libraries built in, needing only \
glibc $GLIBC_FLOOR or newer, built with zig $ZIG_VERSION by \`tools/libarchive/build-jar.sh\` from these sources:

$components

Gradle downloads it from here and checks it against \`gradle/verification-metadata.xml\`.

SHA-256: \`$pinned\`"

download=https://github.com/$repository/releases/download/$release_tag/$asset
downloaded=$(curl -sSfL "$download" | sha256sum | cut -d' ' -f1)
if [ "$downloaded" != "$pinned" ]; then
    echo "the published $asset has SHA-256 $downloaded instead of $pinned" >&2
    exit 1
fi
echo "published https://github.com/$repository/releases/tag/$release_tag"
