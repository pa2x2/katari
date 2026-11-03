/**
 * Katari's published releases, most recently published first, read once while the site builds. Both apps release from pa2x2/katari: the
 * Android app tagged v<version>, the desktop app desktop-v<version>, so each picks its own from this one list rather
 * than from GitHub's single "latest" release.
 */
import { fetchFromGitHub, type GitHubRelease } from "./github.ts";

/** Covers years of releases of both apps, so each finds its newest. */
const RELEASES = "https://api.github.com/repos/pa2x2/katari/releases?per_page=100";

let releases: Promise<GitHubRelease[]> | undefined;

/** Rejects when GitHub can't be reached; each caller decides what its page shows then. */
export function publishedReleases(): Promise<GitHubRelease[]> {
  releases ??= fetchFromGitHub(RELEASES).then((body) =>
    (JSON.parse(body) as GitHubRelease[])
      .filter((release) => !release.draft)
      // GitHub lists releases by when they were created, and a draft can be published after one created later.
      .sort((a, b) => Date.parse(b.published_at) - Date.parse(a.published_at)),
  );
  return releases;
}
