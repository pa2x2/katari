/**
 * The Android app's newest published release, read from GitHub while the site builds, so the download buttons link the
 * APKs themselves. The site workflow runs again when a release is published, which keeps these links current.
 */
import type { GitHubAsset } from "./github.ts";
import { publishedReleases } from "./published-releases.ts";
import type { ReleaseFile } from "./release-format.ts";


/** The two builds of one release made for one processor type, smaller than the universal APKs. */
export interface AbiApks {
  abi: string;
  standard: ReleaseFile;
  foss: ReleaseFile;
}

export interface Release {
  tag: string;
  publishedAt: Date;
  /** The release's page on GitHub, with its notes. */
  page: string;
  /** The universal APKs, which run on every phone. */
  standard: ReleaseFile;
  foss: ReleaseFile;
  /** In the order release.yml builds them; an ABI the release has no files for is left out. */
  abis: AbiApks[];
  /** The release's `katari-<tag>-mpv.json`, which says which mpv add-on it plays with; undefined when it has none. */
  mpvRequirement?: string;
}

/** v<version>, as release.yml tags the Android app; the desktop app's desktop-v tags don't match. */
const ANDROID_TAG = /^v\d/;

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
  // Pre-releases are left out, as GitHub's "latest" release leaves them out.
  const latest = (await publishedReleases()).find((candidate) => !candidate.prerelease && ANDROID_TAG.test(candidate.tag_name));
  if (!latest) throw new Error("GitHub lists no published Android release");
  const tag = latest.tag_name;
  // Named as release.yml names them.
  const asset = (name: string): GitHubAsset | undefined => latest.assets.find((candidate) => candidate.name === name);
  const apk = (name: string): ReleaseFile | undefined => {
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
