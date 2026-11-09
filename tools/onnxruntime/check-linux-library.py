"""Checks that a jar's Linux ONNX Runtime loads on every distribution the desktop app runs on and has no telemetry.

    check-linux-library.py <onnxruntime-linux-x86-64.jar>

The jar may hold the linux-x64 libraries only. Each may need only glibc, the C++ runtime and ONNX Runtime itself (the
JNI library loads libonnxruntime.so by its SONAME), with no symbol newer than the floor: glibc 2.28 and the libstdc++
of GCC 8 (GLIBCXX 3.4.25, CXXABI 1.3.11), which AlmaLinux 8, the build system, has. It must not contain the collector
address of Microsoft's 1DS telemetry client, which builds with telemetry link.
"""

import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

if len(sys.argv) != 2:
    sys.exit(__doc__)
jar = Path(sys.argv[1])

LIBRARIES = {
    "libc.so.6", "libm.so.6", "libpthread.so.0", "libdl.so.2", "librt.so.1", "ld-linux-x86-64.so.2",
    "libstdc++.so.6", "libgcc_s.so.1",
}
FLOORS = {"GLIBC": (2, 28), "GLIBCXX": (3, 4, 25), "CXXABI": (1, 3, 11)}
TELEMETRY = b"events.data.microsoft.com"

problems = []
with zipfile.ZipFile(jar) as archive, tempfile.TemporaryDirectory() as scratch:
    platform = "ai/onnxruntime/native/linux-x64/"
    names = [name for name in archive.namelist() if name.endswith(".so")]
    problems += [f"holds {name}, which is not for linux-x64" for name in names if not name.startswith(platform)]
    if platform + "libonnxruntime.so" not in names:
        problems.append("holds no linux-x64 libonnxruntime.so")
    libraries = {name: Path(archive.extract(name, scratch)) for name in names}
    allowed = set(LIBRARIES)
    if platform + "libonnxruntime.so" in libraries:
        dynamic = subprocess.run(["readelf", "-W", "--dynamic", libraries[platform + "libonnxruntime.so"]],
                                 check=True, capture_output=True, text=True)
        allowed.update(re.findall(r"\(SONAME\)\s+Library soname: \[([^]]+)\]", dynamic.stdout))
    for name, library in libraries.items():
        if TELEMETRY in library.read_bytes():
            problems.append(f"{name} contains telemetry")
        dynamic = subprocess.run(["readelf", "-W", "--dynamic", library], check=True, capture_output=True, text=True)
        needed = set(re.findall(r"\(NEEDED\)\s+Shared library: \[([^]]+)\]", dynamic.stdout))
        problems += [f"{name} needs {other}" for other in sorted(needed - allowed)]
        symbols = subprocess.run(["readelf", "-W", "--dyn-syms", library], check=True, capture_output=True, text=True)
        newest = {}
        for prefix, version in re.findall(r"@+(GLIBCXX|GLIBC|CXXABI)_([0-9.]+)", symbols.stdout):
            parts = tuple(int(part) for part in version.split("."))
            newest[prefix] = max(newest.get(prefix, parts), parts)
        for prefix, version in sorted(newest.items()):
            if version > FLOORS[prefix]:
                problems.append(f"{name} uses {prefix} {'.'.join(map(str, version))} symbols")
        print(f"{name}: needs {', '.join(sorted(needed))}; " +
              ", ".join(f"{prefix} up to {'.'.join(map(str, version))}" for prefix, version in sorted(newest.items())))

if problems:
    sys.exit(f"{jar}:\n  " + "\n  ".join(problems))
