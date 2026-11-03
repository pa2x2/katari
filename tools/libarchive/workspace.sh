# Sourced by the libarchive scripts next to it: what they build, where they keep downloads and builds, and where they
# stage the jar. The jar's version comes from gradle/libs.versions.toml: the libarchive version, with a -<n> suffix for
# later builds of it (a published jar is never replaced, so a rebuild for another zlib, say, needs new coordinates).
#
# Each source is pinned by its SHA-256, so a replaced download cannot change what gets built. To move one, change its
# version and URL, run the build once to see the new download's checksum fail, check it against the project's
# published checksum or signature, and record it here.

# The oldest glibc the library may need. The JetBrains Runtime the AppImage ships needs 2.28, so a lower floor gains
# nothing and a higher one would drop distributions the app runs on today.
GLIBC_FLOOR=2.28

ZIG_VERSION=0.17.0
ZIG_URL=https://ziglang.org/download/$ZIG_VERSION/zig-x86_64-linux-$ZIG_VERSION.tar.xz
ZIG_SHA256=1cbe9df9f27e6b78d14ccbca43b6703a404ef79ef1c463de901d7f088d4e2026

# name version url sha256; libarchive first, then the libraries built into it.
SOURCES=(
    "libarchive 3.8.11 https://github.com/libarchive/libarchive/releases/download/v3.8.11/libarchive-3.8.11.tar.xz a3dc0872b1b39da34dc18ef1b944f0f11ffd37425d70ae04be679279f1cf43d2"
    "zlib 1.3.2 https://github.com/madler/zlib/releases/download/v1.3.2/zlib-1.3.2.tar.xz d7a0654783a4da529d1bb793b7ad9c3318020af77667bcae35f95d0e42a792f3"
    "bzip2 1.0.8 https://sourceware.org/pub/bzip2/bzip2-1.0.8.tar.gz ab5a03176ee106d3f0fa90e381da478ddae405918153cca248e682cd0c4a2269"
    "xz 5.8.4 https://github.com/tukaani-project/xz/releases/download/v5.8.4/xz-5.8.4.tar.xz 4ce24038fd4221e0d13bc1a2de7a4db56e90b92b3bf75321f6c14be73f65de4b"
    "zstd 1.5.7 https://github.com/facebook/zstd/releases/download/v1.5.7/zstd-1.5.7.tar.gz eb33e51f49a15e023950cd7825ca74a4a2b43db8354825ac24fc1b7ee09e6fa3"
    "lz4 1.10.0 https://github.com/lz4/lz4/releases/download/v1.10.0/lz4-1.10.0.tar.gz 537512904744b35e232912055ccf8ec66d768639ff3abe5788d90d792ec5f48b"
)

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
jar_version=$(sed -n 's/^libarchive-desktop = "\([^"]*\)".*/\1/p' "$root/gradle/libs.versions.toml")
work=${LIBARCHIVE_BUILD_DIR:-$HOME/.cache/katari-libarchive}
release_tag=libarchive-$jar_version
# settings.gradle.kts reads the staged jar from here with -PlocalLibarchive=true, in the layout of the release
# download URL.
staged_jar=$work/release/$release_tag/libarchive-linux-x86-64-$jar_version.jar

# Prints field $2 of source $1: 1 for its version, 2 for its URL, 3 for its SHA-256.
source_field() {
    local entry
    for entry in "${SOURCES[@]}"; do
        read -r -a fields <<< "$entry"
        if [ "${fields[0]}" = "$1" ]; then
            echo "${fields[$2]}"
            return
        fi
    done
    echo "no source $1" >&2
    exit 1
}

# Downloads $1 to $2 unless it is there, and stops unless it has the SHA-256 $3.
fetch() {
    if [ ! -f "$2" ]; then
        mkdir -p "$(dirname "$2")"
        curl -sSfL -o "$2.part" "$1"
        mv "$2.part" "$2"
    fi
    local actual
    actual=$(sha256sum < "$2" | cut -d' ' -f1)
    if [ "$actual" != "$3" ]; then
        echo "$1 has SHA-256 $actual, not the pinned $3" >&2
        exit 1
    fi
}
