"""Records the SHA-256 of the desktop libmpv jar in Gradle's dependency verification metadata.

    record-checksum.py <verification-metadata.xml> <version> <sha256>

Replaces the libmpv-linux-x86-64 component, whatever its version, so the file only ever pins the jar the build uses,
and leaves the Android AAR's component of the same group alone.
"""

import re
import sys
from pathlib import Path

if len(sys.argv) != 4:
    sys.exit(__doc__)
path, version, digest = Path(sys.argv[1]), sys.argv[2], sys.argv[3]
component = f"""      <component group="app.katari.libmpv" name="libmpv-linux-x86-64" version="{version}">
         <artifact name="libmpv-linux-x86-64-{version}.jar">
            <sha256 value="{digest}" origin="build-jar.sh"/>
         </artifact>
      </component>
"""
text = path.read_text()
pattern = re.compile(r'      <component group="app\.katari\.libmpv" name="libmpv-linux-x86-64".*?</component>\n', re.S)
if pattern.search(text):
    text = pattern.sub(lambda _: component, text)
else:
    text = text.replace("   </components>", component + "   </components>")
if component not in text:
    sys.exit(f"could not record the checksum in {path}")
path.write_text(text)
