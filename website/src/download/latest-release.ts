/**
 * The newest published release, read from GitHub while the site builds, so the download buttons link the APKs
 * themselves. The site workflow runs again when a release is published, which keeps these links current.
 */
import { fetchFromGitHub, type GitHubAsset, type GitHubRelease } from "./github.ts";

export interface Apk {
  url: string;
  bytes: number;
}

/** The two builds of one release made for one processor type, smaller than the universal APKs. */
export interface AbiApks {
  abi: string;
  standard: Apk;
  foss: Apk;
}

export interface Release {
  tag: string;
  publishedAt: Date;
  /** The release's page on GitHub, with its notes. */
  page: string;
  /** The universal APKs, which run on every phone. */
  standard: Apk;
  foss: Apk;
  /** In the order release.yml builds them; an ABI the release has no files for is left out. */
  abis: AbiApks[];
  /** The release's `katari-<tag>-mpv.json`, which says which mpv add-on it plays with; undefined when it has none. */
  mpvRequirement?: string;
}

/** The ABIs release.yml builds APKs for, from releasedAbis in app/build.gradle.kts. */
export const RELEASED_ABIS = ["arm64-v8a", "armeabi-v7a"];

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
  const latest = JSON.parse(await fetchFromGitHub("https://api.github.com/repos/pa2x2/katari/releases/latest")) as GitHubRelease;
  const tag = latest.tag_name;
  // Named as release.yml names them.
  const asset = (name: string): GitHubAsset | undefined => latest.assets.find((candidate) => candidate.name === name);
  const apk = (name: string): Apk | undefined => {
    const found = asset(name);
    return found && { url: found.browser_download_url, bytes: found.size };
  };
  const standard = apk(`katari-${tag}.apk`);
  const foss = apk(`katari-${tag}-foss.apk`);
  if (!standard || !foss) throw new Error(`${tag} has no universal APKs`);
  return {
    tag,
    publishedAt: new Date(latest.published_at),
    page: latest.html_url,
    standard,
    foss,
    abis: RELEASED_ABIS.flatMap((abi) => {
      const abiStandard = apk(`katari-${abi}-${tag}.apk`);
      const abiFoss = apk(`katari_foss_${abi}-${tag}.apk`);
      return abiStandard && abiFoss ? [{ abi, standard: abiStandard, foss: abiFoss }] : [];
    }),
    mpvRequirement: asset(`katari-${tag}-mpv.json`)?.browser_download_url,
  };
}

// Sized the way the GitHub release page the buttons link to sizes it: binary megabytes to three significant digits,
// so 81,700,586 bytes read 77.9 MB in both places rather than 82 MB here.
export function megabytes(apk: Apk): string {
  return `${Number((apk.bytes / 1024 ** 2).toPrecision(3))} MB`;
}

export function releaseDate(date: Date): string {
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" });
}
