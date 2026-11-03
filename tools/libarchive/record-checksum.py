"""Records the SHA-256 of the desktop libarchive jar in Gradle's dependency verification metadata.

    record-checksum.py <verification-metadata.xml> <version> <sha256>

Replaces the app.katari.libarchive component, whatever its version, so the file only ever pins the jar the build uses.
"""

import re
import sys
from pathlib import Path

if len(sys.argv) != 4:
    sys.exit(__doc__)
path, version, digest = Path(sys.argv[1]), sys.argv[2], sys.argv[3]
component = f"""      <component group="app.katari.libarchive" name="libarchive-linux-x86-64" version="{version}">
         <artifact name="libarchive-linux-x86-64-{version}.jar">
            <sha256 value="{digest}" origin="build-jar.sh"/>
         </artifact>
      </component>
"""
text = path.read_text()
pattern = re.compile(r'      <component group="app\.katari\.libarchive".*?</component>\n', re.S)
if pattern.search(text):
    text = pattern.sub(lambda _: component, text)
else:
    text = text.replace("   </components>", component + "   </components>")
if component not in text:
    sys.exit(f"could not record the checksum in {path}")
path.write_text(text)
