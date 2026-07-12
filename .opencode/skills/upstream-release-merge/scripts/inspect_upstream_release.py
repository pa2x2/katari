#!/usr/bin/env python3
"""Resolve an upstream release tag and summarize its delta against the fork."""

from __future__ import annotations

import argparse
import collections
import re
import subprocess
import sys
from pathlib import Path


VERSION_RE = re.compile(r"^v?\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$")
REMOTE_RE = re.compile(r"^[0-9A-Za-z._-]+$")


class InspectionError(RuntimeError):
    pass


def git(*args: str, check: bool = True) -> str:
    process = subprocess.run(
        ["git", *args],
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if check and process.returncode != 0:
        detail = process.stderr.strip() or process.stdout.strip()
        raise InspectionError(f"git {' '.join(args)} failed: {detail}")
    return process.stdout.strip()


def normalize_version(raw_version: str) -> str:
    version = raw_version.strip()
    if not VERSION_RE.fullmatch(version):
        raise InspectionError(
            f"invalid release version {raw_version!r}; expected a semantic tag such as v0.20.1",
        )
    return version if version.startswith("v") else f"v{version}"


def verify_remote(remote: str) -> str:
    if not REMOTE_RE.fullmatch(remote):
        raise InspectionError(f"invalid remote name {remote!r}")
    remotes = git("remote").splitlines()
    if remote not in remotes:
        raise InspectionError(f"Git remote {remote!r} is not configured")
    return git("remote", "get-url", remote)


def resolve_remote_tag(remote: str, tag: str) -> tuple[str, str]:
    tag_ref = f"refs/tags/{tag}"
    output = git("ls-remote", "--tags", remote, tag_ref, f"{tag_ref}^{{}}")
    rows = [line.split(maxsplit=1) for line in output.splitlines() if line.strip()]
    direct = next((sha for sha, ref in rows if ref == tag_ref), None)
    peeled = next((sha for sha, ref in rows if ref == f"{tag_ref}^{{}}"), None)
    if direct is None:
        raise InspectionError(f"tag {tag!r} does not exist on remote {remote!r}")
    return tag_ref, peeled or direct


def ensure_target_ref(remote: str, tag_ref: str, tag: str, expected_commit: str, fetch: bool) -> tuple[str, str]:
    target_ref = f"refs/remotes/{remote}/tags/{tag}"
    if fetch:
        git("fetch", "--quiet", "--no-tags", remote, f"{tag_ref}:{target_ref}")

    resolved = git("rev-parse", "--verify", f"{target_ref}^{{commit}}", check=False)
    if not resolved:
        raise InspectionError(
            f"{target_ref} is unavailable locally; rerun with --fetch to retrieve the exact tag",
        )
    if resolved != expected_commit:
        raise InspectionError(
            f"{target_ref} resolves to {resolved}, but {remote}/{tag} resolves to {expected_commit}",
        )
    return target_ref, resolved


def nul_paths(*diff_args: str) -> list[str]:
    output = git("diff", "--name-only", "-z", *diff_args)
    return [path for path in output.split("\0") if path]


def count_commits(revision_range: str) -> int:
    return int(git("rev-list", "--count", revision_range))


def top_areas(paths: list[str], limit: int = 15) -> list[tuple[str, int]]:
    counts = collections.Counter(path.split("/", 1)[0] for path in paths)
    return counts.most_common(limit)


def print_list(items: list[str], empty_text: str = "None") -> None:
    if not items:
        print(empty_text)
        return
    for item in items:
        print(f"- {item}")


def inspect(args: argparse.Namespace) -> None:
    repository_root = Path(git("rev-parse", "--show-toplevel"))
    version = normalize_version(args.version)
    remote_url = verify_remote(args.remote)
    git("rev-parse", "--verify", f"{args.base}^{{commit}}")

    tag_ref, expected_commit = resolve_remote_tag(args.remote, version)
    target_ref, target_commit = ensure_target_ref(
        args.remote,
        tag_ref,
        version,
        expected_commit,
        args.fetch,
    )
    base_commit = git("rev-parse", f"{args.base}^{{commit}}")
    merge_base = git("merge-base", base_commit, target_commit)

    upstream_range = f"{merge_base}..{target_commit}"
    fork_range = f"{merge_base}..{base_commit}"
    upstream_paths = nul_paths(merge_base, target_commit)
    fork_paths = nul_paths(merge_base, base_commit)
    overlap = sorted(set(upstream_paths) & set(fork_paths))

    print("# Upstream release inspection")
    print()
    print(f"- Repository: `{repository_root}`")
    print(f"- Remote: `{args.remote}` (`{remote_url}`)")
    print(f"- Requested tag: `{version}`")
    print(f"- Target ref: `{target_ref}`")
    print(f"- Target commit: `{target_commit}`")
    print(f"- Fork base ref: `{args.base}`")
    print(f"- Fork base commit: `{base_commit}`")
    print(f"- Merge base: `{merge_base}`")
    print()

    print("## Scope")
    print()
    print(f"- Upstream range: `{upstream_range}` ({count_commits(upstream_range)} commits)")
    print(f"- Fork range: `{fork_range}` ({count_commits(fork_range)} commits)")
    print(f"- Upstream diff: {git('diff', '--shortstat', merge_base, target_commit) or 'no changes'}")
    print(f"- Fork diff: {git('diff', '--shortstat', merge_base, base_commit) or 'no changes'}")
    print(f"- Upstream paths changed: {len(upstream_paths)}")
    print(f"- Fork paths changed: {len(fork_paths)}")
    print(f"- Overlapping paths: {len(overlap)}")
    print()

    print("## Upstream commits")
    print()
    commits = git(
        "log",
        f"--max-count={args.max_commits}",
        "--format=- `%h` %s",
        upstream_range,
    )
    print(commits or "None")
    if count_commits(upstream_range) > args.max_commits:
        print(f"- ... output limited to {args.max_commits} commits")
    print()

    print("## Most affected upstream areas")
    print()
    print_list([f"`{area}`: {count} paths" for area, count in top_areas(upstream_paths)])
    print()

    print("## Paths changed on both sides")
    print()
    print_list([f"`{path}`" for path in overlap])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Resolve an exact upstream release tag and summarize its merge scope.",
    )
    parser.add_argument("version", help="Upstream semantic release, for example v0.20.1")
    parser.add_argument("--remote", default="upstream", help="Upstream Git remote (default: upstream)")
    parser.add_argument("--base", default="main", help="Fork base ref (default: main)")
    parser.add_argument(
        "--fetch",
        action="store_true",
        help="fetch the exact tag into refs/remotes/<remote>/tags/<tag>",
    )
    parser.add_argument(
        "--max-commits",
        type=int,
        default=200,
        help="maximum upstream commits to print (default: 200)",
    )
    args = parser.parse_args()
    if args.max_commits < 1:
        parser.error("--max-commits must be positive")
    return args


def main() -> int:
    try:
        inspect(parse_args())
    except InspectionError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
