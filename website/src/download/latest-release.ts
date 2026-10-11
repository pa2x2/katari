/**
 * The newest published release, read from GitHub while the site builds, so the download buttons link the APKs
 * themselves. The site workflow runs again when a release is published, which keeps these links current.
 */
export interface Apk {
  url: string;
  bytes: number;
}

export interface Release {
  tag: string;
  publishedAt: Date;
  /** The universal APKs, which run on every phone. */
  standard: Apk;
  foss: Apk;
}

interface GitHubRelease {
  tag_name: string;
  published_at: string;
  assets: { name: string; size: number; browser_download_url: string }[];
}

let release: Promise<Release | undefined> | undefined;

/** Undefined when GitHub can't be reached, such as in an offline build; the buttons then open the releases page. */
export function latestRelease(): Promise<Release | undefined> {
  release ??= fetchLatestRelease().catch((error: unknown) => {
    console.warn(`The download buttons link the releases page instead of the APKs: ${error}`);
    return undefined;
  });
  return release;
}

async function fetchLatestRelease(): Promise<Release> {
  const token = process.env.GITHUB_TOKEN;
  const response = await fetch("https://api.github.com/repos/pa2x2/katari/releases/latest", {
    headers: { accept: "application/vnd.github+json", ...(token ? { authorization: `Bearer ${token}` } : {}) },
    // A stalled answer would otherwise hold the whole build; the fallback to the releases page covers it.
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) throw new Error(`GitHub answered ${response.status}`);
  const latest = (await response.json()) as GitHubRelease;
  // Named as release.yml names them.
  const apk = (name: string): Apk => {
    const asset = latest.assets.find((candidate) => candidate.name === name);
    if (!asset) throw new Error(`${latest.tag_name} has no ${name}`);
    return { url: asset.browser_download_url, bytes: asset.size };
  };
  return {
    tag: latest.tag_name,
    publishedAt: new Date(latest.published_at),
    standard: apk(`katari-${latest.tag_name}.apk`),
    foss: apk(`katari-${latest.tag_name}-foss.apk`),
  };
}

export function megabytes(apk: Apk): string {
  return `${Math.round(apk.bytes / 1_000_000)} MB`;
}

export function releaseDate(release: Release): string {
  return release.publishedAt.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" });
}
