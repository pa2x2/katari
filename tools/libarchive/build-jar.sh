#!/usr/bin/env bash
# Builds the libarchive the desktop app reads archives with on x86_64 Linux, stages its jar where
# `-PlocalLibarchive=true` builds read it and publish-release.sh uploads it from, records the jar's SHA-256 in
# gradle/verification-metadata.xml, and writes the licences screen's entries for what the library contains to
# desktop/licences.
#
# The library has zlib, bzip2, xz (liblzma), zstd and lz4 built in and needs nothing but glibc, from GLIBC_FLOOR on, so
# it loads on any x86_64 distribution the app runs on, whatever archive libraries that system has. It exports only
# libarchive's own functions: the compression libraries inside stay private, so they never stand in for, or get
# replaced by, the system's copies other libraries in the process load. zig compiles it, as zig links against any
# glibc version without an old distribution's sysroot.
#
# It reads every format and filter libarchive has without further libraries, which covers what the local source opens:
# zip, RAR (RAR5 included), 7z and tar, and the epubs and comic book archives that are one of those. Left out are the
# crypto libraries (only AES-encrypted zip and 7z need them) and the XML parsers (only xar needs them).
#
# Requires curl, CMake, Ninja, Python 3 and binutils; zig is downloaded at the pinned version.
#
# After changing what it builds (workspace.sh, or the libarchive-desktop version in gradle/libs.versions.toml, which
# needs a -<n> suffix for a rebuild of the same libarchive): run this, try the desktop app with -PlocalLibarchive=true,
# run publish-release.sh, then commit and push.
set -euo pipefail

. "$(dirname "$0")/workspace.sh"

target=x86_64-linux-gnu.$GLIBC_FLOOR
downloads=$work/downloads
build=$work/build
prefix=$build/deps
toolchain=$build/toolchain

zig_home=$work/zig-$ZIG_VERSION
if [ ! -x "$zig_home/zig" ]; then
    fetch "$ZIG_URL" "$downloads/${ZIG_URL##*/}" "$ZIG_SHA256"
    rm -rf "$zig_home" "$zig_home.part"
    mkdir -p "$zig_home.part"
    tar -xf "$downloads/${ZIG_URL##*/}" -C "$zig_home.part" --strip-components=1
    mv "$zig_home.part" "$zig_home"
fi

rm -rf "$build"
mkdir -p "$prefix/lib" "$prefix/include" "$toolchain" "$build/out"
printf '#!/bin/sh\nexec "%s/zig" cc -target %s "$@"\n' "$zig_home" "$target" > "$toolchain/cc"
printf '#!/bin/sh\nexec "%s/zig" ar "$@"\n' "$zig_home" > "$toolchain/ar"
printf '#!/bin/sh\nexec "%s/zig" ranlib "$@"\n' "$zig_home" > "$toolchain/ranlib"
chmod +x "$toolchain/cc" "$toolchain/ar" "$toolchain/ranlib"
# A cross build confined to the prefix: CMake must find the static libraries built here, never this machine's.
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
# libarchive asks pkg-config for some libraries; it may only see the ones built here.
export PKG_CONFIG_LIBDIR=$prefix/lib/pkgconfig PKG_CONFIG_PATH=

# Extracts source $1 into $build/src and prints its directory.
unpack() {
    local version url archive
    version=$(source_field "$1" 1)
    url=$(source_field "$1" 2)
    archive=$downloads/${url##*/}
    fetch "$url" "$archive" "$(source_field "$1" 3)"
    mkdir -p "$build/src"
    tar -xf "$archive" -C "$build/src"
    echo "$build/src/$1-$version"
}

# Configures the CMake project in directory $1, a source from unpack or a directory in one, with the options after it,
# then builds it and installs it into $prefix.
cmake_install() {
    local source=$1 name
    name=${1#"$build/src/"}
    name=${name%%/*}
    shift
    cmake -S "$source" -B "$build/out/$name" -G Ninja --toolchain "$toolchain/toolchain.cmake" \
        -DCMAKE_INSTALL_PREFIX="$prefix" -DCMAKE_INSTALL_LIBDIR=lib "$@" > "$build/out/$name.log"
    cmake --build "$build/out/$name" >> "$build/out/$name.log"
    cmake --install "$build/out/$name" >> "$build/out/$name.log"
}

zlib=$(unpack zlib)
cmake_install "$zlib" -DZLIB_BUILD_SHARED=OFF -DZLIB_BUILD_TESTING=OFF

# bzip2 1.0.8 builds with a plain Makefile only; its library is these six files.
bzip2=$(unpack bzip2)
(
    cd "$bzip2"
    for file in blocksort huffman crctable randtable compress decompress bzlib; do
        "$toolchain/cc" -O2 -fPIC -D_FILE_OFFSET_BITS=64 -c "$file.c" -o "$file.o"
    done
    "$toolchain/ar" rcs "$prefix/lib/libbz2.a" ./*.o
    cp bzlib.h "$prefix/include/"
)

xz=$(unpack xz)
cmake_install "$xz" -DBUILD_SHARED_LIBS=OFF -DXZ_NLS=OFF -DXZ_DOC=OFF -DXZ_TOOL_XZ=OFF -DXZ_TOOL_XZDEC=OFF \
    -DXZ_TOOL_LZMADEC=OFF -DXZ_TOOL_LZMAINFO=OFF -DXZ_TOOL_SCRIPTS=OFF

zstd=$(unpack zstd)
cmake_install "$zstd/build/cmake" -DZSTD_BUILD_SHARED=OFF -DZSTD_BUILD_PROGRAMS=OFF -DZSTD_BUILD_TESTS=OFF \
    -DZSTD_MULTITHREAD_SUPPORT=OFF -DZSTD_LEGACY_SUPPORT=OFF

lz4=$(unpack lz4)
cmake_install "$lz4/build/cmake" -DBUILD_SHARED_LIBS=OFF -DBUILD_STATIC_LIBS=ON -DLZ4_BUILD_CLI=OFF

libarchive=$(unpack libarchive)
# Every library's symbols but libarchive's own stay inside the library.
printf '{\n  global: archive_*;\n  local: *;\n};\n' > "$build/exports.map"
cmake_install "$libarchive" \
    -DBUILD_SHARED_LIBS=ON \
    -DENABLE_OPENSSL=OFF -DENABLE_MBEDTLS=OFF -DENABLE_NETTLE=OFF -DENABLE_CNG=OFF -DENABLE_MD=OFF \
    -DENABLE_LIBB2=OFF -DENABLE_LZO=OFF -DENABLE_LIBXML2=OFF -DENABLE_BSDXML=OFF -DENABLE_EXPAT=OFF \
    -DENABLE_PCREPOSIX=OFF -DENABLE_PCRE2POSIX=OFF -DENABLE_LIBGCC=OFF -DENABLE_ACL=OFF -DENABLE_XATTR=OFF \
    -DENABLE_TAR=OFF -DENABLE_CPIO=OFF -DENABLE_CAT=OFF -DENABLE_UNZIP=OFF -DENABLE_TEST=OFF \
    -DENABLE_ZLIB=ON -DENABLE_BZip2=ON -DENABLE_LZMA=ON -DENABLE_ZSTD=ON -DENABLE_LZ4=ON -DENABLE_ICONV=ON \
    -DPOSIX_REGEX_LIB=LIBC \
    -DICONV_INCLUDE_DIR="$zig_home/lib/libc/include/generic-glibc" \
    -DREGEX_INCLUDE_DIR="$zig_home/lib/libc/include/generic-glibc" \
    -DCMAKE_SHARED_LINKER_FLAGS="-Wl,--version-script=$build/exports.map"

library=$build/libarchive.so
cp "$prefix/lib/libarchive.so.13" "$library"
strip --strip-unneeded "$library"
python3 -I "$here/check-library.py" "$library" "$GLIBC_FLOOR"
python3 -I "$here/verify-library.py" "$library" "$libarchive/libarchive/test"

licences=()
for entry in "${SOURCES[@]}"; do
    read -r -a fields <<< "$entry"
    licences+=("${fields[0]}=${fields[1]}=$build/src/${fields[0]}-${fields[1]}")
done
rm -rf "$(dirname "$staged_jar")"
mkdir -p "$(dirname "$staged_jar")"
python3 -I "$here/pack-jar.py" "$library" "$staged_jar" "$root/desktop/licences" "${licences[@]}"
python3 -I "$here/record-checksum.py" "$root/gradle/verification-metadata.xml" "$jar_version" \
    "$(sha256sum < "$staged_jar" | cut -d' ' -f1)"
echo "staged $staged_jar"
