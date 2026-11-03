"""Checks that a built libarchive.so loads on any distribution with glibc from a given version on.

    check-library.py <libarchive.so> <glibc floor, such as 2.28>

It may need only glibc's own libraries, no glibc symbol newer than the floor, and it must export libarchive's
functions only, so the compression libraries built into it never mix with a system copy another library loads.
"""

import re
import subprocess
import sys

if len(sys.argv) != 3:
    sys.exit(__doc__)
library, floor = sys.argv[1], tuple(int(part) for part in sys.argv[2].split("."))

GLIBC_LIBRARIES = {
    "libc.so.6", "libm.so.6", "libpthread.so.0", "libdl.so.2", "librt.so.1", "ld-linux-x86-64.so.2",
}


def readelf(*arguments):
    return subprocess.run(["readelf", "-W", *arguments, library], check=True, capture_output=True, text=True).stdout


problems = []
needed = set(re.findall(r"\(NEEDED\)\s+Shared library: \[([^]]+)\]", readelf("--dynamic")))
problems += [f"needs {name}, which is not part of glibc" for name in sorted(needed - GLIBC_LIBRARIES)]

newest = (0,)
exported = []
for line in readelf("--dyn-syms").splitlines():
    fields = line.split()
    if len(fields) < 8 or not fields[0].endswith(":"):
        continue
    binding, section, name = fields[4], fields[6], fields[7]
    version = re.search(r"@+GLIBC_([0-9.]+)", name)
    if version:
        newest = max(newest, tuple(int(part) for part in version.group(1).split(".")))
    if section != "UND" and binding in ("GLOBAL", "WEAK") and not name.split("@")[0].startswith("archive_"):
        exported.append(name)
if newest > floor:
    problems.append(f"uses glibc {'.'.join(map(str, newest))} symbols, newer than {sys.argv[2]}")
problems += [f"exports {name}" for name in sorted(exported)]

if problems:
    sys.exit(f"{library}:\n  " + "\n  ".join(problems))
print(f"{library}: needs {', '.join(sorted(needed))}; glibc symbols up to {'.'.join(map(str, newest))}")
