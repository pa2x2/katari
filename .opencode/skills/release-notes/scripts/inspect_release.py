#!/usr/bin/env python3
"""Validate a Katari release range and print its metadata as JSON."""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path


VERSION = re.compile(r"^(?:v)?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")


def run(*args: str, check: bool = True) -> str:
    result = subprocess.run(args, check=False, text=True, capture_output=True)
    if check and result.returncode:
        message = result.stderr.strip() or result.stdout.strip() or "command failed"
        raise RuntimeError(f"{' '.join(args)}: {message}")
    return result.stdout.strip()


def version_tuple(tag: str) -> tuple[int, int, int]:
    match = VERSION.fullmatch(tag)
    if not match:
        raise ValueError(f"invalid stable semantic version: {tag}")
    return tuple(int(part) for part in match.groups())


def app_version(root: Path) -> str:
    build_file = root / "app" / "build.gradle.kts"
    match = re.search(r'^\s*versionName\s*=\s*"([^"]+)"', build_file.read_text(), re.MULTILINE)
    if not match:
        raise RuntimeError("could not find versionName in app/build.gradle.kts")
    return match.group(1)


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: inspect_release.py <version>", file=sys.stderr)
        return 2

    requested = version_tuple(sys.argv[1])
    tag = f"v{'.'.join(str(part) for part in requested)}"
    root = Path(run("git", "rev-parse", "--show-toplevel"))

    target_ref = f"refs/tags/{tag}"
    target_sha = run("git", "rev-parse", "--verify", f"{target_ref}^{{commit}}")

    candidates: list[tuple[tuple[int, int, int], str]] = []
    for candidate in run("git", "tag", "--list", "v*").splitlines():
        try:
            parsed = version_tuple(candidate)
        except ValueError:
            continue
        if parsed >= requested:
            continue
        ancestor = subprocess.run(
            ["git", "merge-base", "--is-ancestor", candidate, tag],
            check=False,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if ancestor.returncode == 0:
            candidates.append((parsed, candidate))

    if not candidates:
        raise RuntimeError(f"no previous local stable Katari release tag is an ancestor of {tag}")
    previous_tag = max(candidates)[1]
    configured_version = app_version(root)
    if configured_version != tag.removeprefix("v"):
        raise RuntimeError(
            f"app version {configured_version} does not match target tag {tag}"
        )

    print(
        json.dumps(
            {
                "version": tag.removeprefix("v"),
                "tag": tag,
                "target_sha": target_sha,
                "previous_tag": previous_tag,
                "range": f"{previous_tag}..{tag}",
                "app_version": configured_version,
            },
            indent=2,
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, ValueError) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1)
