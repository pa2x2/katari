#!/usr/bin/env python3
"""Validate a Katari release range and print its metadata as JSON."""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlparse


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


def origin_release_tags() -> dict[str, str]:
    tags: dict[str, str] = {}
    peeled: dict[str, str] = {}
    output = run("git", "ls-remote", "--tags", "origin", "refs/tags/v*")
    for line in output.splitlines():
        sha, ref = line.split(maxsplit=1)
        if ref.endswith("^{}"):
            peeled[ref.removesuffix("^{}")] = sha
        else:
            tags[ref] = sha
    tags.update(peeled)
    return tags


def origin_github_repo() -> str:
    remote_url = run("git", "remote", "get-url", "origin")
    if "://" not in remote_url and ":" in remote_url:
        remote_url = f"ssh://{remote_url.replace(':', '/', 1)}"

    parsed = urlparse(remote_url)
    path = parsed.path.strip("/").removesuffix(".git")
    parts = path.split("/")
    if not parsed.hostname or len(parts) != 2 or not all(parts):
        raise RuntimeError(f"could not determine GitHub repository from origin URL: {remote_url}")

    return "/".join((parsed.hostname, *parts))


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: inspect_release.py <version>", file=sys.stderr)
        return 2

    requested = version_tuple(sys.argv[1])
    tag = f"v{'.'.join(str(part) for part in requested)}"
    root = Path(run("git", "rev-parse", "--show-toplevel"))

    target_ref = f"refs/tags/{tag}"
    target_sha = run("git", "rev-parse", "--verify", f"{target_ref}^{{commit}}")
    remote_tags = origin_release_tags()
    if target_ref not in remote_tags:
        raise RuntimeError(f"target tag {tag} does not exist on origin")
    if remote_tags[target_ref] != target_sha:
        raise RuntimeError(f"local tag {tag} does not match origin")

    candidates: list[tuple[tuple[int, int, int], str]] = []
    for candidate_ref, remote_sha in remote_tags.items():
        candidate = candidate_ref.removeprefix("refs/tags/")
        try:
            parsed = version_tuple(candidate)
        except ValueError:
            continue
        if parsed >= requested:
            continue
        local_sha = run("git", "rev-parse", "--verify", f"{candidate_ref}^{{commit}}", check=False)
        if not local_sha or local_sha != remote_sha:
            continue
        ancestor = subprocess.run(
            ["git", "merge-base", "--is-ancestor", candidate, target_ref],
            check=False,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if ancestor.returncode == 0:
            candidates.append((parsed, candidate))

    if not candidates:
        raise RuntimeError(f"no previous stable Katari release tag is an ancestor of {tag}")
    previous_tag = max(candidates)[1]
    configured_version = app_version(root)
    if configured_version != tag.removeprefix("v"):
        raise RuntimeError(
            f"app version {configured_version} does not match target tag {tag}"
        )

    release = json.loads(
        run(
            "gh",
            "release",
            "view",
            tag,
            "--repo",
            origin_github_repo(),
            "--json",
            "tagName,name,isDraft,isPrerelease,body,url",
        )
    )
    if release["tagName"] != tag:
        raise RuntimeError(f"GitHub returned unexpected release tag {release['tagName']}")
    if not release["isDraft"]:
        raise RuntimeError(f"GitHub release {tag} is not a draft")

    print(
        json.dumps(
            {
                "version": tag.removeprefix("v"),
                "tag": tag,
                "target_sha": target_sha,
                "previous_tag": previous_tag,
                "range": f"{previous_tag}..{tag}",
                "app_version": configured_version,
                "release": release,
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
