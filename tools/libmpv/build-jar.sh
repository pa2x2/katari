#!/usr/bin/env bash
# Builds the libmpv the desktop app plays video with on x86_64 Linux, stages its jar where `-PlocalLibmpv=true` builds
# read it and publish-release.sh uploads it from, records the jar's SHA-256 in gradle/verification-metadata.xml, and
# writes the licences screen's entries for what the library contains to desktop/licences.
#
# It is the mpv the Android app's add-on runs: the same sources, versions and patches, from the source bundle of the
# pa2x2/mpv-android release workspace.sh names, and an LGPL build like that one (FFmpeg without --enable-gpl, mpv with
# -Dgpl=false). Built in and kept private: FFmpeg with dav1d, libxml2 (FFmpeg's DASH demuxer) and zlib, libass with
# FreeType, HarfBuzz, FriBidi and libunibreak, libplacebo, and libdrm with libdisplay-info. It exports only mpv's API
# (client.h and render.h) and needs nothing but glibc, from GLIBC_FLOOR on, so it loads on any x86_64 distribution the
# app runs on. zig compiles it, as zig links against any glibc version without an old distribution's sysroot.
#
# What carries drivers or the desktop's configuration stays the system's, opened when first used (see
# system-libraries.txt), so a system without one only loses that feature: VA-API (libva, libva-drm), EGL for VA-API's
# zero-copy path, PipeWire, PulseAudio and ALSA for audio, and fontconfig for libass's system fonts. FFmpeg opens the
# Vulkan loader itself for Vulkan decoding (hwdec vulkan-copy). OpenGL comes from the caller through the render API.
#
# Left out, as in the Android build: the mpv player, Lua and JavaScript scripting, libcurl, FFmpeg's encoders, muxers
# and devices. Also left out on the desktop: TLS, since the app hands mpv every stream through its loopback HTTP proxy
# (video-engine/mpv's StreamProxy); X11 and Wayland, which an LGPL mpv builds no video output or VA-API display for (mpv
# opens VA-API on the GPU's DRM render node instead, which the render API's DRM display parameter or a standalone
# hwdec=vaapi-copy uses under X11 and Wayland alike); and mpv's Vulkan renderer, which the render API cannot use.
#
# Requires curl, CMake, Meson, Ninja, NASM, pkg-config, Python 3, GNU patch, make and binutils for the build, and
# FFmpeg's command line tool with libx264 for the test media verify-library.py makes; zig is downloaded at the pinned
# version.
#
# After changing what it builds (workspace.sh, system-libraries.txt, or the libmpv-desktop version in
# gradle/libs.versions.toml, which needs a .<n> suffix for a rebuild of the same fork release): run this, try the
# desktop app with -PlocalLibmpv=true, run publish-release.sh, then commit and push.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

if [ "$jar_version" != "$FORK_VERSION" ] && [[ ! "$jar_version" =~ ^$FORK_VERSION\.[0-9]+$ ]]; then
    echo "libmpv-desktop $jar_version in gradle/libs.versions.toml is not $FORK_VERSION or a .<n> rebuild of it" >&2
    exit 1
fi

target=x86_64-linux-gnu.$GLIBC_FLOOR
downloads=$work/downloads
build=$work/build
prefix=$build/deps
toolchain=$build/toolchain
src=$build/src
# Native builds share the machine with others, so they take half of it unless told otherwise.
jobs=${LIBMPV_BUILD_JOBS:-$(( ($(nproc) + 1) / 2 ))}

zig_home=$work/zig-$ZIG_VERSION
if [ ! -x "$zig_home/zig" ]; then
    fetch "$ZIG_URL" "$downloads/${ZIG_URL##*/}" "$ZIG_SHA256"
    rm -rf "$zig_home" "$zig_home.part"
    mkdir -p "$zig_home.part"
    tar -xf "$downloads/${ZIG_URL##*/}" -C "$zig_home.part" --strip-components=1
    mv "$zig_home.part" "$zig_home"
fi

rm -rf "$build"
mkdir -p "$prefix/lib/pkgconfig" "$prefix/include" "$toolchain" "$build/out" "$src"
printf '#!/bin/sh\nexec "%s/zig" cc -target %s "$@"\n' "$zig_home" "$target" > "$toolchain/cc"
printf '#!/bin/sh\nexec "%s/zig" c++ -target %s "$@"\n' "$zig_home" "$target" > "$toolchain/c++"
printf '#!/bin/sh\nexec "%s/zig" ar "$@"\n' "$zig_home" > "$toolchain/ar"
printf '#!/bin/sh\nexec "%s/zig" ranlib "$@"\n' "$zig_home" > "$toolchain/ranlib"
# FFmpeg's configure compiles Vulkan shaders with any GLSL compiler it finds on the PATH. None of what this build keeps
# needs one, and this machine's must not decide what gets built.
for compiler in glslc glslang glslangValidator; do
    printf '#!/bin/sh\nexit 1\n' > "$toolchain/$compiler"
done
chmod +x "$toolchain"/*
export PATH=$toolchain:$PATH
# A cross build confined to the prefix: the builds may find only the libraries and headers built or installed here,
# never this machine's.
export PKG_CONFIG_LIBDIR=$prefix/lib/pkgconfig PKG_CONFIG_PATH= PKG_CONFIG_SYSROOT_DIR=
cat > "$toolchain/toolchain.cmake" << EOF
set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR x86_64)
set(CMAKE_C_COMPILER $toolchain/cc)
set(CMAKE_AR $toolchain/ar)
set(CMAKE_RANLIB $toolchain/ranlib)
set(CMAKE_FIND_ROOT_PATH $prefix)
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_BUILD_TYPE Release)
EOF
# cmake = false keeps Meson from looking for dependencies through CMake, which searches this machine's libraries.
cat > "$toolchain/cross.ini" << EOF
[binaries]
c = '$toolchain/cc'
cpp = '$toolchain/c++'
ar = '$toolchain/ar'
ranlib = '$toolchain/ranlib'
strip = 'strip'
pkg-config = 'pkg-config'
cmake = 'false'
nasm = 'nasm'

[properties]
pkg_config_libdir = '$prefix/lib/pkgconfig'

[built-in options]
buildtype = 'release'
default_library = 'static'
prefer_static = true
b_staticpic = true
b_pie = false
wrap_mode = 'nodownload'

[host_machine]
system = 'linux'
cpu_family = 'x86_64'
cpu = 'x86_64'
endian = 'little'
EOF

# Runs the command after $1 with its output in log $1, and shows the end of the log when it fails.
logged() {
    local log=$1
    shift
    if ! "$@" >> "$log" 2>&1; then
        tail -n 40 "$log" >&2
        echo "failed: $* (log: $log)" >&2
        exit 1
    fi
}

# Extracts archive $1 into $src and prints the directory it made.
extract() {
    local top
    top=$(tar -tf "$1" | sed -n 1p)
    tar -xf "$1" -C "$src"
    echo "$src/${top%%/*}"
}

# Extracts desktop source $1 (see SOURCES) and prints its directory.
unpack() {
    local url name archive
    url=$(source_field "$1" 2)
    name=${url##*/}
    [[ "$name" == "$1"* ]] || name=$1-$name
    archive=$downloads/$name
    fetch "$url" "$archive" "$(source_field "$1" 3)"
    extract "$archive"
}

fetch "$FORK_SOURCES_URL" "$downloads/${FORK_SOURCES_URL##*/}" "$FORK_SOURCES_SHA256"
fork=$build/fork
mkdir -p "$fork"
tar -xf "$downloads/${FORK_SOURCES_URL##*/}" -C "$fork"
depinfo=$fork/mpv-android/buildscripts/include/depinfo.sh
# Prints the fork's pinned version $1 (v_<name> in its depinfo.sh).
fork_version() {
    local value
    value=$(sed -n "s/^v_$1=//p" "$depinfo")
    [ -n "$value" ] || { echo "the fork's depinfo.sh pins no v_$1" >&2; exit 1; }
    echo "$value"
}

# Extracts the fork's source archive $1 and applies the fork's patches for it (patches/$2), and prints its directory.
# The fork applies them with patch's default fuzz; here every hunk must apply exactly, so a patch that drifted fails
# instead of landing in the wrong place.
unpack_fork() {
    local directory patch
    [ -f "$fork/upstream/$1" ] || { echo "the fork's sources have no $1" >&2; exit 1; }
    directory=$(extract "$fork/upstream/$1")
    if [ -n "${2:-}" ]; then
        for patch in "$fork/mpv-android/buildscripts/patches/$2"/*.patch; do
            [ -f "$patch" ] || continue
            logged "$build/out/patches.log" patch -d "$directory" -p1 --forward --batch --fuzz=0 -i "$patch"
            if grep -q -E "with fuzz|offset" "$build/out/patches.log"; then
                echo "$patch does not apply exactly" >&2
                exit 1
            fi
        done
    fi
    echo "$directory"
}

# Configures the Meson project in directory $1 with the options after it.
meson_configure() {
    local source=$1 name
    name=$(basename "$source")
    shift
    logged "$build/out/$name.log" meson setup "$build/out/$name" "$source" --cross-file "$toolchain/cross.ini" \
        --prefix "$prefix" --libdir lib "$@"
}

# Builds the Meson project configured for directory $1 and installs it into $prefix.
meson_build() {
    local name
    name=$(basename "$1")
    logged "$build/out/$name.log" ninja -C "$build/out/$name" -j "$jobs"
    logged "$build/out/$name.log" meson install -C "$build/out/$name" --no-rebuild
}

meson_install() {
    meson_configure "$@"
    meson_build "$1"
}

# The configuration strings FFmpeg and mpv carry, and the directories they would read data and configuration from,
# name the build directory. Rewritten in configuration header $1 to a fixed directory that exists nowhere, they keep
# this machine's paths out of the library and make it the same wherever it was built, and mpv's global configuration
# directory stays empty.
neutral_paths() {
    sed -i "s|$build/|/katari-libmpv-build/|g" "$1"
    if grep -q "$build" "$1"; then
        echo "$1 still names $build" >&2
        exit 1
    fi
}

# Configures the CMake project in directory $1 with the options after it, then builds it and installs it into $prefix.
cmake_install() {
    local source=$1 name
    name=$(basename "$source")
    shift
    logged "$build/out/$name.log" cmake -S "$source" -B "$build/out/$name" -G Ninja \
        --toolchain "$toolchain/toolchain.cmake" -DCMAKE_INSTALL_PREFIX="$prefix" -DCMAKE_INSTALL_LIBDIR=lib "$@"
    logged "$build/out/$name.log" cmake --build "$build/out/$name" -j "$jobs"
    logged "$build/out/$name.log" cmake --install "$build/out/$name"
}

# Configures the autotools project in directory $1 out of tree with the options after it, then builds it and installs
# it into $prefix.
configure_install() {
    local source=$1 name
    name=$(basename "$source")
    shift
    mkdir -p "$build/out/$name"
    (
        cd "$build/out/$name"
        logged "$build/out/$name.log" "$source/configure" --host=x86_64-linux-gnu --prefix="$prefix" --with-pic \
            --enable-static --disable-shared CC="$toolchain/cc" AR="$toolchain/ar" RANLIB="$toolchain/ranlib" "$@"
        logged "$build/out/$name.log" make -j "$jobs"
        logged "$build/out/$name.log" make install
    )
}

# Writes pkg-config file $1 with the description, cflags and libs after it.
pkg_config_file() {
    cat > "$prefix/lib/pkgconfig/$1.pc" << EOF
prefix=$prefix
includedir=\${prefix}/include
libdir=\${prefix}/lib

Name: $1
Description: $2
Version: $3
Cflags: $4
Libs: $5
EOF
}

# Builds the stand-in for system library $1 (see lazy-library.py) as static library lib$2.a.
lazy_library() {
    python3 -I "$here/lazy-library.py" "$here/system-libraries.txt" "$1" "$build/out/lazy-$2.c"
    "$toolchain/cc" -O2 -fPIC -c "$build/out/lazy-$2.c" -o "$build/out/lazy-$2.o"
    "$toolchain/ar" rcs "$prefix/lib/lib$2.a" "$build/out/lazy-$2.o"
}

# Libraries built in.

zlib=$(unpack zlib)
cmake_install "$zlib" -DZLIB_BUILD_SHARED=OFF -DZLIB_BUILD_TESTING=OFF

# FFmpeg's DASH demuxer reads manifests with it; the fork's options.
libxml2=$(unpack_fork "libxml2-$(fork_version libxml2).tar.xz")
meson_install "$libxml2" -Dminimum=true -D{push,reader,sax1,iso8859x,pattern}=enabled

dav1d=$(unpack_fork "dav1d-$(fork_version dav1d).tar.xz")
meson_install "$dav1d" -Denable_tools=false -Denable_tests=false -Denable_examples=false

freetype=$(unpack_fork "freetype-$(fork_version freetype).tar.xz")
meson_install "$freetype" -Dzlib=enabled -D{brotli,bzip2,harfbuzz,png}=disabled -Dtests=disabled

fribidi=$(unpack_fork "fribidi-$(fork_version fribidi).tar.xz")
meson_install "$fribidi" -D{bin,tests,docs}=false

harfbuzz=$(unpack_fork "harfbuzz-$(fork_version harfbuzz).tar.xz")
meson_install "$harfbuzz" -Dfreetype=enabled -D{glib,gobject,cairo,chafa,icu,graphite2,png,zlib}=disabled \
    -D{tests,docs,utilities,introspection,benchmark}=disabled -D{raster,vector,gpu,subset}=disabled

unibreak=$(unpack_fork "libunibreak-$(fork_version unibreak).tar.gz")
configure_install "$unibreak"

libdrm=$(unpack libdrm)
meson_install "$libdrm" -D{intel,radeon,amdgpu,nouveau,vmwgfx,omap,exynos,freedreno,tegra,vc4,etnaviv}=disabled \
    -D{cairo-tests,man-pages,valgrind}=disabled -D{tests,udev,install-test-programs}=false

# mpv's DRM support needs it, for the display outputs mpv itself drives. It names monitors' vendors from hwdata's
# list of PNP IDs, which a player rendering into the app's window never shows: an empty list keeps this machine's out
# of the build.
libdisplay_info=$(unpack libdisplay-info)
mkdir -p "$build/hwdata/pkgconfig"
: > "$build/hwdata/pnp.ids"
printf 'pkgdatadir=%s\n\nName: hwdata\nDescription: none\nVersion: 0\n' "$build/hwdata" \
    > "$build/hwdata/pkgconfig/hwdata.pc"
meson_install "$libdisplay_info" -Dbuild.pkg_config_path="$build/hwdata/pkgconfig"

# Libraries taken from the system: their headers, pkg-config files naming the stand-ins, and the stand-ins.

libva=$(unpack libva)
mkdir -p "$prefix/include/va"
cp "$libva"/va/*.h "$libva"/va/drm/*.h "$prefix/include/va/"
va_api=$(sed -n -E 's/^va_api_(major|minor|micro)_version = ([0-9]+)$/\2/p' "$libva/meson.build" | paste -sd.)
sed -e "s/@VA_API_MAJOR_VERSION@/${va_api%%.*}/" -e "s/@VA_API_MINOR_VERSION@/$(cut -d. -f2 <<< "$va_api")/" \
    -e "s/@VA_API_MICRO_VERSION@/${va_api##*.}/" -e "s/@VA_API_VERSION@/$va_api/" \
    "$libva/va/va_version.h.in" > "$prefix/include/va/va_version.h"
lazy_library libva.so.2 lazy-va
lazy_library libva-drm.so.2 lazy-va-drm
pkg_config_file libva "libva from the system" "$va_api" "-I\${includedir}" "-L\${libdir} -llazy-va -ldl -lpthread"
pkg_config_file libva-drm "libva-drm from the system" "$va_api" "-I\${includedir}" \
    "-L\${libdir} -llazy-va-drm -llazy-va -ldl -lpthread"

egl=$(unpack EGL-Registry)
cp -r "$egl/api/EGL" "$egl/api/KHR" "$prefix/include/"
lazy_library libEGL.so.1 lazy-egl
# EGL_NO_X11: an X11 Display is only an opaque pointer here.
pkg_config_file egl "EGL from the system" 1.5 "-I\${includedir} -DEGL_NO_X11" "-L\${libdir} -llazy-egl -ldl -lpthread"

pipewire=$(unpack pipewire)
pipewire_version=$(source_field pipewire 1)
mkdir -p "$prefix/include/pipewire-0.3" "$prefix/include/spa-0.2"
(cd "$pipewire/src" && find pipewire -name '*.h' -exec cp --parents {} "$prefix/include/pipewire-0.3/" \;)
cp -r "$pipewire/spa/include/spa" "$prefix/include/spa-0.2/"
IFS=. read -r pw_major pw_minor pw_micro <<< "$pipewire_version"
sed -e "s/@PIPEWIRE_VERSION_MAJOR@/$pw_major/g" -e "s/@PIPEWIRE_VERSION_MINOR@/$pw_minor/g" \
    -e "s/@PIPEWIRE_VERSION_MICRO@/$pw_micro/g" -e 's/@PIPEWIRE_API_VERSION@/"0.3"/g' \
    "$pipewire/src/pipewire/version.h.in" > "$prefix/include/pipewire-0.3/pipewire/version.h"
lazy_library libpipewire-0.3.so.0 lazy-pipewire
pkg_config_file libpipewire-0.3 "PipeWire from the system" "$pipewire_version" \
    "-I\${includedir}/pipewire-0.3 -I\${includedir}/spa-0.2 -D_REENTRANT" "-L\${libdir} -llazy-pipewire -ldl -lpthread"

pulseaudio=$(unpack pulseaudio)
mkdir -p "$prefix/include/pulse"
cp "$pulseaudio"/src/pulse/*.h "$prefix/include/pulse/"
pa_api=$(sed -n 's/^pa_api_version = \([0-9]*\)$/\1/p' "$pulseaudio/meson.build")
pa_protocol=$(sed -n 's/^pa_protocol_version = \([0-9]*\)$/\1/p' "$pulseaudio/meson.build")
pa_version=$(source_field pulseaudio 1)
sed -e "s/@PA_MAJOR@/${pa_version%%.*}/g" -e "s/@PA_MINOR@/${pa_version##*.}/g" -e "s/@PA_API_VERSION@/$pa_api/g" \
    -e "s/@PA_PROTOCOL_VERSION@/$pa_protocol/g" "$pulseaudio/src/pulse/version.h.in" > "$prefix/include/pulse/version.h"
lazy_library libpulse.so.0 lazy-pulse
pkg_config_file libpulse "PulseAudio from the system" "$pa_version" "-I\${includedir} -D_REENTRANT" \
    "-L\${libdir} -llazy-pulse -ldl -lpthread"

alsa=$(unpack alsa-lib)
mkdir -p "$prefix/include/alsa"
cp "$alsa"/include/*.h "$prefix/include/alsa/"
cp -r "$alsa/include/sound" "$prefix/include/alsa/"
lazy_library libasound.so.2 lazy-asound
pkg_config_file alsa "ALSA from the system" "$(source_field alsa-lib 1)" "-I\${includedir}" \
    "-L\${libdir} -llazy-asound -ldl -lpthread"

fontconfig=$(unpack_fork "fontconfig-$(fork_version fontconfig).tar.xz")
mkdir -p "$prefix/include/fontconfig"
cp "$fontconfig"/fontconfig/*.h "$prefix/include/fontconfig/"
# Its cache format numbers, which only fontconfig itself reads.
cache_number() { sed -n "s/^$1 = \([0-9]*\)$/\1/p" "$fontconfig/meson.build"; }
cache_version=$(cache_number cacheversion)
next_cache_version=$(( cache_version + ($(cache_number cachesnapversion) > 0 ? 1 : 0) ))
sed -e "s/@CACHE_VERSION@/$cache_version/" -e "s/@CACHE_SNAP_VERSION@/$(cache_number cachesnapversion)/" \
    -e "s/@NEXT_CACHE_VERSION@/$next_cache_version/" -e "s/@CACHE_MIN_COMPAT_VERSION@/$(cache_number cachemincompat)/" \
    "$fontconfig/fontconfig/fontconfig.h.in" > "$prefix/include/fontconfig/fontconfig.h"
lazy_library libfontconfig.so.1 lazy-fontconfig
pkg_config_file fontconfig "fontconfig from the system" "$(fork_version fontconfig)" "-I\${includedir}" \
    "-L\${libdir} -llazy-fontconfig -ldl -lpthread"

# Libraries built in that use the ones above.

libass=$(unpack_fork "libass-$(fork_version libass).tar.xz")
configure_install "$libass" --enable-asm --enable-fontconfig --enable-libunibreak

libplacebo=$(extract "$fork/upstream/libplacebo-v$(fork_version libplacebo).tar.gz")
meson_install "$libplacebo" -Dvulkan=disabled -Dopengl=enabled -Dgl-proc-addr=disabled \
    -D{d3d11,glslang,shaderc,lcms,libdovi,unwind,xxhash}=disabled -D{demos,tests,bench,fuzz}=false
# libplacebo is partly C++: whatever links it needs zig's C++ runtime, which goes into the library statically.
sed -i '/^Libs:/ s|$| -lc++|' "$prefix/lib/pkgconfig/libplacebo.pc"
# FFmpeg's Vulkan decoding needs only Vulkan's headers: it opens the system's loader itself.
cp -r "$libplacebo/3rdparty/Vulkan-Headers/include/vulkan" "$libplacebo/3rdparty/Vulkan-Headers/include/vk_video" \
    "$prefix/include/"

ffmpeg=$(unpack_fork "ffmpeg-$(fork_version ffmpeg).tar.xz" ffmpeg)
mkdir -p "$build/out/ffmpeg"
(
    cd "$build/out/ffmpeg"
    # The fork's options but TLS, on a desktop: VA-API and Vulkan decoding, libdrm for VA-API's DRM frames, iconv
    # from glibc for subtitle character sets. Nothing else is detected, so this machine's libraries stay out.
    logged "$build/out/ffmpeg.log" "$ffmpeg/configure" --prefix="$prefix" \
        --enable-cross-compile --target-os=linux --arch=x86_64 --cc="$toolchain/cc" --cxx="$toolchain/c++" \
        --ar="$toolchain/ar" --ranlib="$toolchain/ranlib" --nm=nm --strip=strip --pkg-config=pkg-config \
        --pkg-config-flags=--static --enable-static --disable-shared --enable-pic \
        --disable-autodetect --enable-pthreads --enable-zlib --enable-iconv \
        --enable-libdrm --enable-vaapi --enable-vulkan --enable-libdav1d --enable-libxml2 \
        --disable-{doc,programs} --disable-{muxers,encoders,devices,avdevice}
    for feature in CONFIG_VAAPI HAVE_VAAPI_DRM CONFIG_VULKAN CONFIG_LIBDRM CONFIG_LIBDAV1D CONFIG_LIBXML2 CONFIG_ZLIB \
        CONFIG_ICONV; do
        grep -q "^#define $feature 1" config.h || { echo "FFmpeg was configured without $feature" >&2; exit 1; }
    done
    # zig's glibc 2.28 still has sysctl() to link against, but its headers, glibc's newest, no longer have
    # sys/sysctl.h. FFmpeg would only use it on the BSDs.
    sed -i 's/^#define HAVE_SYSCTL 1$/#define HAVE_SYSCTL 0/' config.h
    neutral_paths config.h
    logged "$build/out/ffmpeg.log" make -j "$jobs"
    logged "$build/out/ffmpeg.log" make install
)

mpv=$(unpack_fork "mpv-$(fork_version mpv).tar.gz" mpv)
# Every library's symbols but mpv's API stay inside the library.
printf '{\n  global: mpv_*;\n  local: *;\n};\n' > "$build/exports.map"
meson_configure "$mpv" --default-library=shared -Dauto_features=disabled \
    -Dgpl=false -Dlibmpv=true -Dcplayer=false -D{manpage,html,pdf}-build=disabled -Dbuild-date=false \
    -D{zlib,vector,gl,plain-gl,egl,drm,vaapi,vaapi-drm,pipewire,pulse,alsa}=enabled \
    -Dc_link_args="-Wl,--version-script=$build/exports.map"
neutral_paths "$build/out/$(basename "$mpv")/config.h"
meson_build "$mpv"

library=$build/libmpv.so
cp "$(readlink -f "$prefix/lib/libmpv.so")" "$library"
strip --strip-unneeded "$library"
python3 -I "$here/check-library.py" "$library" "$GLIBC_FLOOR" "$here/system-libraries.txt"
python3 -I "$here/verify-library.py" "$library" "$work/test-media" "$toolchain/cc"

mpv_commit=${mpv##*mpv-}
mpv_description="$(fork_version mpv_base) (git ${mpv_commit:0:9}, $(fork_version mpv_date))"
licences=(
    "mpv=$mpv_description=$mpv"
    "FFmpeg=$(fork_version ffmpeg)=$ffmpeg"
    "dav1d=$(fork_version dav1d)=$dav1d"
    "libxml2=$(fork_version libxml2)=$libxml2"
    "zlib=$(source_field zlib 1)=$zlib"
    "libass=$(fork_version libass)=$libass"
    "FreeType=$(fork_version freetype)=$freetype"
    "HarfBuzz=$(fork_version harfbuzz)=$harfbuzz"
    "FriBidi=$(fork_version fribidi)=$fribidi"
    "libunibreak=$(fork_version unibreak)=$unibreak"
    "libplacebo=$(fork_version libplacebo)=$libplacebo"
    "fast_float=$(fork_version libplacebo)=$libplacebo/3rdparty/fast_float"
    "glad=$(fork_version libplacebo)=$libplacebo/3rdparty/glad"
    "libdrm=$(source_field libdrm 1)=$libdrm"
    "libdisplay-info=$(source_field libdisplay-info 1)=$libdisplay_info"
    "libc++=zig $ZIG_VERSION=$zig_home/lib"
)
rm -rf "$(dirname "$staged_jar")"
mkdir -p "$(dirname "$staged_jar")"
python3 -I "$here/pack-jar.py" "$library" "$staged_jar" "$root/desktop/licences" "${licences[@]}"
python3 -I "$here/record-checksum.py" "$root/gradle/verification-metadata.xml" "$jar_version" \
    "$(sha256sum < "$staged_jar" | cut -d' ' -f1)"
echo "staged $staged_jar"
