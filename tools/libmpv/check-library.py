"""Checks that a built libmpv.so loads on any distribution with glibc from a given version on.

    check-library.py <libmpv.so> <glibc floor, such as 2.28> <system-libraries.txt>

It may need only glibc's own libraries, no glibc symbol newer than the floor and no symbol from anywhere else: the
system libraries it uses are opened when first used (lazy-library.py), never linked, so a missing one cannot keep it
from loading. It must export mpv's functions only, so the libraries built into it never mix with a system copy another
library in the process loads, and its stand-ins for the system libraries must not stand in for anyone else.
"""

import re
import subprocess
import sys
from pathlib import Path

if len(sys.argv) != 4:
    sys.exit(__doc__)
library, floor = sys.argv[1], tuple(int(part) for part in sys.argv[2].split("."))
system_functions = {
    line.split()[0]
    for line in Path(sys.argv[3]).read_text().splitlines()
    if line[:1].isspace() and line.split("#", 1)[0].strip()
}

GLIBC_LIBRARIES = {
    "libc.so.6", "libm.so.6", "libpthread.so.0", "libdl.so.2", "librt.so.1", "ld-linux-x86-64.so.2",
}


def readelf(*arguments):
    return subprocess.run(["readelf", "-W", *arguments, library], check=True, capture_output=True, text=True).stdout


problems = []
dynamic = readelf("--dynamic")
needed = set(re.findall(r"\(NEEDED\)\s+Shared library: \[([^]]+)\]", dynamic))
problems += [f"needs {name}, which is not part of glibc" for name in sorted(needed - GLIBC_LIBRARIES)]
problems += [f"has a {kind}" for kind in ("RPATH", "RUNPATH") if f"({kind})" in dynamic]

newest = (0,)
exported, unbound = [], []
for line in readelf("--dyn-syms").splitlines():
    fields = line.split()
    if len(fields) < 8 or not fields[0].endswith(":"):
        continue
    binding, section, name = fields[4], fields[6], fields[7]
    bare = name.split("@")[0]
    version = re.search(r"@+GLIBC_([0-9.]+)", name)
    if version:
        newest = max(newest, tuple(int(part) for part in version.group(1).split(".")))
    if section == "UND":
        # Undefined weak symbols without a version are the toolchain's optional hooks, which stay null.
        if not version and not (binding == "WEAK" and "@" not in name):
            unbound.append(name)
    elif binding in ("GLOBAL", "WEAK") and not bare.startswith("mpv_"):
        exported.append(name)
if newest > floor:
    problems.append(f"uses glibc {'.'.join(map(str, newest))} symbols, newer than {sys.argv[2]}")
problems += [f"exports {name}" for name in sorted(exported)]
problems += [
    f"needs {name} from {'a system library it should open itself' if name in system_functions else 'outside glibc'}"
    for name in sorted(unbound)
]

if problems:
    sys.exit(f"{library}:\n  " + "\n  ".join(problems))
print(f"{library}: needs {', '.join(sorted(needed))}; glibc symbols up to {'.'.join(map(str, newest))}")
