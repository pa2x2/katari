"""Downloads every ONNX model of the text recognition provider into a directory, named as probe-models.sh expects.

    fetch-models.py <models-dir>

The models, their pinned URLs and their SHA-256 digests are read from OnnxModelArtifacts.kt and PaddleOcrScript.kt, so
the probes always cover what the app downloads. Files already present with the right digest are kept, and models the
catalog no longer lists are deleted.
"""

import hashlib
import re
import sys
import urllib.request
from pathlib import Path

SOURCES = Path(__file__).resolve().parents[2] / "src/main/java/mihon/text/recognition/provider/onnx"


def constants(source):
    return dict(re.findall(r'const val (\w+) = "([^"]*)"', source))


def expand(template, values):
    return re.sub(r"\$\{?(\w+)\}?", lambda match: values[match.group(1)], template)


def catalog():
    artifacts = (SOURCES / "catalog/OnnxModelArtifacts.kt").read_text()
    scripts = (SOURCES / "paddle/PaddleOcrScript.kt").read_text()
    values = constants(artifacts)
    models = {}
    for url, digest in re.findall(r'url = "([^"]+)",\s*sizeBytes = [\d_]+,\s*sha256 = "(\w+)"', artifacts):
        if url.startswith("$folder/"):
            continue
        url = expand(url, values)
        if url.endswith(".onnx"):
            models[url.rsplit("/", 1)[1]] = (url, digest)
    folder_url = re.search(r'val folder = "([^"]+)"', artifacts).group(1)
    for folder, digest in re.findall(r'folder = "(\w+)",.*?recognizerSha256 = "(\w+)"', scripts, re.S):
        folder_path = expand(folder_url.replace("${script.folder}", folder), values)
        url = f"{folder_path}/{values['PADDLE_OCR_RECOGNIZER_FILE']}"
        models[f"rec_{folder}.onnx"] = (url, digest)
    entries = len(re.findall(r"^    \w+\($", scripts, re.M))
    if entries == 0 or entries != sum(name.startswith("rec_") for name in models):
        sys.exit(f"read {len(models)} models but PaddleOcrScript has {entries} entries; update {Path(__file__).name}")
    return models


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for block in iter(lambda: file.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    directory = Path(sys.argv[1])
    directory.mkdir(parents=True, exist_ok=True)
    models = catalog()
    for stale in directory.glob("*.onnx"):
        if stale.name not in models:
            stale.unlink()
    for name, (url, digest) in sorted(models.items()):
        target = directory / name
        if target.exists() and sha256(target) == digest:
            continue
        print(f"downloading {name}", flush=True)
        partial = target.with_suffix(".part")
        urllib.request.urlretrieve(url, partial)
        if sha256(partial) != digest:
            partial.unlink()
            sys.exit(f"{name} from {url} does not match its SHA-256 {digest}")
        partial.rename(target)


main()
