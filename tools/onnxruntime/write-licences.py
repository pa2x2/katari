"""Writes the licences screen's entries for the reduced ONNX Runtime, which Gradle resolves without a POM.

    write-licences.py <ONNX Runtime source> <version> <repository root>

Both apps get one library entry for their own package (the Android AAR in app/licences, the Linux jar in
desktop/licences) under ONNX Runtime's MIT licence and the third-party notices Microsoft publishes with it, which name
every library ONNX Runtime can build in, so they cover what the reduced builds keep.
"""

import json
import sys
from pathlib import Path

if len(sys.argv) != 4:
    sys.exit(__doc__)
source, version, root = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])

LICENCES = {
    "onnxruntime-mit": ("MIT License", source / "LICENSE"),
    "onnxruntime-third-party-notices": ("ONNX Runtime third-party notices", source / "ThirdPartyNotices.txt"),
}
PACKAGES = {
    "app/licences": ("onnxruntime-android", "Runs the on-device text recognition models."),
    "desktop/licences": ("onnxruntime-linux-x86-64", "Runs the on-device text recognition models in the desktop app."),
}


def write(path: Path, entry: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(entry, ensure_ascii=False, indent=2) + "\n")


for directory, (artifact, description) in PACKAGES.items():
    licences = root / directory
    for slug, (name, text) in LICENCES.items():
        write(licences / "licenses" / f"{slug}.json", {"hash": slug, "name": name, "content": text.read_text()})
    write(
        licences / "libraries" / "onnxruntime.json",
        {
            "uniqueId": f"app.katari.onnxruntime:{artifact}",
            "artifactVersion": version,
            "name": "ONNX Runtime",
            "description": description,
            "website": "https://onnxruntime.ai",
            "licenses": list(LICENCES),
        },
    )
