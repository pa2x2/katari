# Sourced by the libmpv scripts next to it: what they build, where they keep downloads and builds, and where they
# stage the jar.
#
# mpv and what it shares with the Android build (FFmpeg, dav1d, libass with FreeType, HarfBuzz, FriBidi and
# libunibreak, libplacebo, libxml2), with the fork's patches to them, come from the source bundle of one release of
# pa2x2/mpv-android, the fork that builds Android's libmpv. The bundle holds the exact archives and patches that
# release was built from, and its depinfo.sh their versions, so both apps run one mpv. The jar's version, from
# gradle/libs.versions.toml, is that release's, with a .<n> suffix for later builds of it (a published jar is never
# replaced, so a rebuild for another libva, say, needs new coordinates).
#
# Every download is pinned by its SHA-256, so a replaced download cannot change what gets built. To move one, change
# its version and URL, run the build once to see the new download's checksum fail, check it against the project's
# published checksum or signature, and record it here. The bundle's checksum is in the fork release's SHA256SUMS.

# The oldest glibc the library may need. The JetBrains Runtime the AppImage ships needs 2.28, so a lower floor gains
# nothing and a higher one would drop distributions the app runs on today.
GLIBC_FLOOR=2.28

ZIG_VERSION=0.17.1
ZIG_URL=https://ziglang.org/download/$ZIG_VERSION/zig-x86_64-linux-$ZIG_VERSION.tar.xz
ZIG_SHA256=3581863e3cbcfd0fce0efc682ce7e9e93da96668a977791d356a26d2a7e9b17a

FORK_VERSION=2026-11-07-1
FORK_SOURCES_URL=https://github.com/pa2x2/mpv-android/releases/download/libmpv-$FORK_VERSION/libmpv-android-$FORK_VERSION-sources.tar
FORK_SOURCES_SHA256=1c2d9194c324376c7ee1c595623e51612ca5645bf8f5cc19e657a25432e03356

# name version url sha256; what the desktop build needs beyond the fork's sources. zlib, libdrm and libdisplay-info are
# built in. The rest give only the headers of libraries libmpv takes from the system (see system-libraries.txt): libva
# for VA-API, PipeWire, PulseAudio and ALSA for audio, EGL for VA-API's zero-copy path into OpenGL.
SOURCES=(
    "zlib 1.4 https://github.com/madler/zlib/releases/download/v1.4/zlib-1.4.tar.xz 4f11c512053d7bab8d9e9b89bde3575add3e4c50a383b42ef6cb837035fc5a7c"
    "libdrm 2.4.134 https://dri.freedesktop.org/libdrm/libdrm-2.4.134.tar.xz ac5e74d157830eb8bee44c6a6bf3ad49774ef0dd2a72bdad74a8f20308b52a95"
    "libdisplay-info 0.4.0 https://gitlab.freedesktop.org/-/project/14497/uploads/289d58230e2981b51f23d31faf25bef9/libdisplay-info-0.4.0.tar.xz 43b180baa143e2035654759d84e2b2f5ee77d5fe817c423838c7fe59c0d68459"
    "libva 2.24.1 https://github.com/intel/libva/releases/download/2.24.1/libva-2.24.1.tar.bz2 eec6050b52876f229bd35e9df17cd31a06785e18e6f7990c445b584628483d67"
    "pipewire 1.6.11 https://gitlab.freedesktop.org/pipewire/pipewire/-/archive/1.6.11/pipewire-1.6.11.tar.bz2 0b32fbbece4591376b45dde5a818bfa5b3517e70913a2f7414e33c47c20b07ee"
    "pulseaudio 17.0 https://freedesktop.org/software/pulseaudio/releases/pulseaudio-17.0.tar.xz 053794d6671a3e397d849e478a80b82a63cb9d8ca296bd35b73317bb5ceb87b5"
    "alsa-lib 1.2.16.1 https://www.alsa-project.org/files/pub/lib/alsa-lib-1.2.16.1.tar.bz2 f740db7f488255944ffd4428416ee3390a96742856916433df468c281436480e"
    "EGL-Registry e964211ff5f7c2bcada786954a16d5b813379395 https://github.com/KhronosGroup/EGL-Registry/archive/e964211ff5f7c2bcada786954a16d5b813379395.tar.gz 6e5eb863035ad893062a592d7a405c321b2dbc4a821c6f5c607a29adbfd86163"
)

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
jar_version=$(sed -n 's/^libmpv-desktop = "\([^"]*\)".*/\1/p' "$root/gradle/libs.versions.toml")
work=${LIBMPV_BUILD_DIR:-$HOME/.cache/katari-libmpv}
release_tag=libmpv-desktop-$jar_version
# settings.gradle.kts reads the staged jar from here with -PlocalLibmpv=true, in the layout of the release download URL.
staged_jar=$work/release/$release_tag/libmpv-linux-x86-64-$jar_version.jar

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
        curl -sSfL --retry 5 --retry-all-errors -o "$2.part" "$1"
        mv "$2.part" "$2"
    fi
    local actual
    actual=$(sha256sum < "$2" | cut -d' ' -f1)
    if [ "$actual" != "$3" ]; then
        echo "$1 has SHA-256 $actual, not the pinned $3" >&2
        exit 1
    fi
}
