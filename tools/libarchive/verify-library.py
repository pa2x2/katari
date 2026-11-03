"""Reads archives of every kind the desktop app opens through a built libarchive.so, as the app does.

    verify-library.py <libarchive.so> <libarchive's libarchive/test directory>

The archives come from libarchive's own tests. Each must list its entries and give their data without an error, which
fails when a format or compression library did not make it into the build: the build skips one it cannot find. A zip
with Shift-JIS names, as older Japanese comic archives have, must give them as UTF-8, which needs the build's iconv.
"""

import binascii
import ctypes
import sys
import tempfile
from pathlib import Path

if len(sys.argv) != 3:
    sys.exit(__doc__)
library_path, tests = sys.argv[1], Path(sys.argv[2])

ARCHIVES = [
    # Comic book archives: cbz, cbr (RAR 4 and RAR 5), cb7 and cbt.
    "test_read_format_zip_7075_utf8_paths.zip.uu",
    "test_read_format_zip_zstd.zipx.uu",
    "test_read_format_zip_xz_multi.zipx.uu",
    "test_read_format_zip_lzma.zipx.uu",
    "test_read_format_rar.rar.uu",
    "test_read_format_rar5_compressed.rar.uu",
    "test_read_format_rar5_solid.rar.uu",
    "test_read_format_7zip_lzma2.7z.uu",
    "test_read_format_7zip_bzip2.7z.uu",
    "test_read_format_7zip_deflate.7z.uu",
    "test_read_format_7zip_ppmd.7z.uu",
    "test_read_format_7zip_zstd.7z.uu",
    "test_compat_gzip_1.tgz.uu",
    "test_compat_bzip2_1.tbz.uu",
    "test_compat_xz_1.txz.uu",
    "test_compat_zstd_1.tar.zst.uu",
    "test_compat_lz4_1.tar.lz4.uu",
]
SHIFT_JIS_ARCHIVE = ("test_read_format_zip_filename_cp932.zip.uu", "CP932")
LIBRARY_VERSIONS = ["zlib", "bzlib", "liblzma", "libzstd", "liblz4"]
ARCHIVE_EOF = 1
ARCHIVE_WARN = -20

lib = ctypes.CDLL(library_path)
for name in ["archive_version_details", "archive_error_string"] + [f"archive_{v}_version" for v in LIBRARY_VERSIONS]:
    getattr(lib, name).restype = ctypes.c_char_p
lib.archive_error_string.argtypes = [ctypes.c_void_p]
lib.archive_read_new.restype = ctypes.c_void_p
for name in ["archive_read_support_filter_all", "archive_read_support_format_all", "archive_read_free"]:
    getattr(lib, name).argtypes = [ctypes.c_void_p]
lib.archive_read_open_filename.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_size_t]
lib.archive_read_next_header.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_void_p)]
lib.archive_read_data.argtypes = [ctypes.c_void_p, ctypes.c_void_p, ctypes.c_size_t]
lib.archive_read_data.restype = ctypes.c_ssize_t
lib.archive_read_set_options.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
lib.archive_entry_pathname_utf8.argtypes = [ctypes.c_void_p]
lib.archive_entry_pathname_utf8.restype = ctypes.c_char_p

print(lib.archive_version_details().decode())
missing = [name for name in LIBRARY_VERSIONS if not getattr(lib, f"archive_{name}_version")()]
if missing:
    sys.exit(f"{library_path} was built without {', '.join(missing)}")


def uudecode(source: Path) -> bytes:
    lines = source.read_text().splitlines()
    start = next(index for index, line in enumerate(lines) if line.startswith("begin "))
    data = bytearray()
    for line in lines[start + 1:]:
        if line == "end":
            return bytes(data)
        if line:
            length = (ord(line[0]) - 32) & 0o77
            data += binascii.a2b_uu(line[:1 + (length + 2) // 3 * 4])
    raise ValueError(f"{source} has no end line")


def read(path: Path, charset: str | None = None) -> list[str]:
    """The UTF-8 names of the archive's entries, after reading each one's data."""
    archive = lib.archive_read_new()
    try:
        def check(result):
            if result < ARCHIVE_WARN:
                raise RuntimeError(lib.archive_error_string(archive).decode(errors="replace"))
            return result

        check(lib.archive_read_support_filter_all(archive))
        check(lib.archive_read_support_format_all(archive))
        if charset:
            check(lib.archive_read_set_options(archive, f"hdrcharset={charset}".encode()))
        check(lib.archive_read_open_filename(archive, str(path).encode(), 64 * 1024))
        entry = ctypes.c_void_p()
        buffer = ctypes.create_string_buffer(64 * 1024)
        names = []
        while check(lib.archive_read_next_header(archive, ctypes.byref(entry))) != ARCHIVE_EOF:
            name = lib.archive_entry_pathname_utf8(entry)
            if name is None:
                raise RuntimeError("an entry's name has no UTF-8 form")
            names.append(name.decode())
            while check(lib.archive_read_data(archive, buffer, len(buffer))) > 0:
                pass
        return names
    finally:
        lib.archive_read_free(archive)


failures = []
with tempfile.TemporaryDirectory() as scratch:
    for name, charset in [(name, None) for name in ARCHIVES] + [SHIFT_JIS_ARCHIVE]:
        path = Path(scratch) / name.removesuffix(".uu")
        path.write_bytes(uudecode(tests / name))
        try:
            names = read(path, charset)
            if not names:
                failures.append(f"{path.name}: no entries")
            elif charset and not any(ord(character) > 0x7F for entry in names for character in entry):
                failures.append(f"{path.name}: names not converted from {charset}: {names}")
        except RuntimeError as error:
            failures.append(f"{path.name}: {error}")
if failures:
    sys.exit("archives the build cannot read:\n  " + "\n  ".join(failures))
print(f"read {len(ARCHIVES) + 1} archives")
