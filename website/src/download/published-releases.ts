/**
 * Katari's published releases, most recently published first, read once while the site builds. The apps and the sync
 * server release from pa2x2/katari: the Android app tagged v<version>, the desktop app desktop-v<version>, the server
 * sync-server-v<version>, so each picks its own from this one list rather than from GitHub's single "latest" release.
 */
import { fetchFromGitHub, type GitHubRelease } from "./github.ts";

/** Covers years of releases of the apps and the server, so each finds its newest. */
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

/**
 * The release the site offers among those tagged [prefix]: the newest stable one, or, while there is none, the newest
 * pre-release, so an alpha is offered until the first stable version and a later beta never replaces a stable one.
 */
export async function offeredRelease(prefix: string): Promise<GitHubRelease | undefined> {
  const tagged = (await publishedReleases()).filter((candidate) => candidate.tag_name.startsWith(prefix));
  return tagged.find((candidate) => !candidate.prerelease) ?? tagged[0];
}
