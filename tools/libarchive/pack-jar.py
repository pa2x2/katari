"""Packs a built libarchive.so into the jar the desktop app loads it from, and writes the licences screen's entries.

    pack-jar.py <libarchive.so> <jar> <licences directory> <name>=<version>=<source directory>...

The library sits at JNA's resource path for x86_64 Linux. The jar carries the licence of each component built into the
library, as their licences ask of a binary, and the licences directory gets one AboutLibraries library and licence
entry per component, in the layout the desktop app's AboutLibraries configuration reads. The libarchive entry takes the
jar's own coordinates, so it stands in for the entry AboutLibraries finds for the jar, which has no POM to describe it.
Entry times are fixed, so the same library always packs into the same jar.
"""

import json
import shutil
import sys
import zipfile
from pathlib import Path

if len(sys.argv) < 5:
    sys.exit(__doc__)
library, jar, licences = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
components = [argument.split("=", 2) for argument in sys.argv[4:]]

JAR_ID = "app.katari.libarchive:libarchive-linux-x86-64"
# name: (licence file in the source, licence name, website, description)
COMPONENTS = {
    "libarchive": (
        "COPYING", "BSD 2-Clause License", "https://www.libarchive.org",
        "Reads the archives of local series in the desktop app.",
    ),
    "zlib": ("LICENSE", "zlib License", "https://zlib.net", "Built into the desktop app's libarchive."),
    "bzip2": (
        "LICENSE", "bzip2 and libbzip2 License", "https://sourceware.org/bzip2/",
        "Built into the desktop app's libarchive.",
    ),
    "xz": (
        "COPYING.0BSD", "BSD Zero Clause License", "https://tukaani.org/xz/",
        "liblzma, built into the desktop app's libarchive.",
    ),
    "zstd": (
        "LICENSE", "BSD 3-Clause License", "https://facebook.github.io/zstd/",
        "Built into the desktop app's libarchive.",
    ),
    "lz4": ("lib/LICENSE", "BSD 2-Clause License", "https://lz4.org", "Built into the desktop app's libarchive."),
}
FIXED_TIME = (2000, 1, 1, 0, 0, 0)

if sorted(name for name, _, _ in components) != sorted(COMPONENTS):
    sys.exit(f"pack-jar.py describes {sorted(COMPONENTS)}, not {sorted(name for name, _, _ in components)}")


def add(archive: zipfile.ZipFile, name: str, data: bytes, mode: int = 0o644):
    info = zipfile.ZipInfo(name, FIXED_TIME)
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = mode << 16
    archive.writestr(info, data)


shutil.rmtree(licences, ignore_errors=True)
(licences / "libraries").mkdir(parents=True)
(licences / "licenses").mkdir()
with zipfile.ZipFile(jar, "w") as archive:
    add(archive, "META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\r\nCreated-By: tools/libarchive\r\n\r\n")
    add(archive, "linux-x86-64/libarchive.so", library.read_bytes(), 0o755)
    for name, version, source in components:
        licence_file, licence_name, website, description = COMPONENTS[name]
        text = (Path(source) / licence_file).read_text()
        add(archive, f"META-INF/licences/{name}.txt", text.encode())
        slug = f"libarchive-desktop-{name}"
        (licences / "licenses" / f"{slug}.json").write_text(
            json.dumps({"hash": slug, "name": licence_name, "content": text}, ensure_ascii=False, indent=2) + "\n",
        )
        library_entry = {
            "uniqueId": JAR_ID if name == "libarchive" else f"app.katari.libarchive:{name}",
            "artifactVersion": version,
            "name": name,
            "description": description,
            "website": website,
            "licenses": [slug],
        }
        (licences / "libraries" / f"{slug}.json").write_text(
            json.dumps(library_entry, ensure_ascii=False, indent=2) + "\n",
        )
