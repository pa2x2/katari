"""Records the SHA-256 of the reduced ONNX Runtime libraries in Gradle's dependency verification metadata.

    record-checksum.py <verification-metadata.xml> <version> <aar sha256> <linux jar sha256>

Replaces every app.katari.onnxruntime component, whatever its version, so the file only ever pins the libraries the
build uses.
"""

import re
import sys
from pathlib import Path

if len(sys.argv) != 5:
    sys.exit(__doc__)
path, version, aar_digest, jar_digest = Path(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4]


def component(name, extension, digest):
    return f"""      <component group="app.katari.onnxruntime" name="{name}" version="{version}">
         <artifact name="{name}-{version}.{extension}">
            <sha256 value="{digest}" origin="build.sh"/>
         </artifact>
      </component>
"""


components = component("onnxruntime-android", "aar", aar_digest) + component(
    "onnxruntime-linux-x86-64", "jar", jar_digest
)
text = path.read_text()
pattern = re.compile(r'      <component group="app\.katari\.onnxruntime".*?</component>\n', re.S)
first = pattern.search(text)
if first:
    text = text[: first.start()] + components + pattern.sub("", text[first.start() :])
else:
    text = text.replace("   </components>", components + "   </components>")
if components not in text:
    sys.exit(f"could not record the checksums in {path}")
path.write_text(text)
