#!/usr/bin/env bash
# Publishes the Chromium archive as the GitHub release the desktop app downloads it from, after checking that it is the
# archive gradle/katari.versions.toml pins. Build it first with ./gradlew :desktop:assembleChromiumArchive. Push the
# commit that pins it only once the release exists: an app built from that commit can't get its web view without it.
#
# The release is never marked latest, so the website's download link and GitHub's latest release stay on the app, and
# its tag starts with neither "v" nor "desktop-v", so the apps' update checks and the release-triggered workflows ignore
# it. Never delete one: the app versions that pin its archive have no web view without it.
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
pin() { sed -n "s/^$1 = \"\([^\"]*\)\".*/\1/p" "$root/gradle/katari.versions.toml"; }
version=$(pin chromium)
pinned=$(pin chromium-linux-x64-sha256)
size=$(pin chromium-linux-x64-size)
release_tag=chromium-$version
asset=chromium-$version-linux-x86_64.tar.zst
archive=$root/desktop/build/chromium/$asset

repository=$(git -C "$root" remote get-url origin | sed -E 's#^(git@github\.com:|https://github\.com/)##; s#\.git$##')
if [ ! -f "$archive" ] || [ "$(sha256sum < "$archive" | cut -d' ' -f1)" != "$pinned" ] ||
    [ "$(stat -c %s "$archive")" != "$size" ]; then
    echo "$archive is missing or not the archive gradle/katari.versions.toml pins;" \
        "run ./gradlew :desktop:assembleChromiumArchive" >&2
    exit 1
fi
if gh release view "$release_tag" --repo "$repository" > /dev/null 2>&1; then
    echo "$repository already has a release $release_tag" >&2
    exit 1
fi

gh release create "$release_tag" "$archive" --repo "$repository" --latest=false \
    --title "Chromium $version for Katari's desktop app (web view download)" \
    --notes "Not an app release. Chromium for x86_64 Linux as JetBrains builds it for the JCEF of the JetBrains \
Runtime, unchanged, packed from the runtime Katari's desktop app runs on by \`:desktop:assembleChromiumArchive\`. Its \
licences are in the archive's \`LICENSES\` folder.

Katari's desktop app downloads it from here when a web view is first needed and checks it against the checksum it was \
built with.

SHA-256: \`$pinned\`"

download=https://github.com/$repository/releases/download/$release_tag/$asset
downloaded=$(curl -sSfL "$download" | sha256sum | cut -d' ' -f1)
if [ "$downloaded" != "$pinned" ]; then
    echo "the published $asset has SHA-256 $downloaded instead of $pinned" >&2
    exit 1
fi
echo "published https://github.com/$repository/releases/tag/$release_tag"
