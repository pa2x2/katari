import { fetchFromGitHub, type GitHubRelease } from "./github.ts";
import { latestRelease, RELEASED_ABIS } from "./latest-release.ts";

/**
 * The mpv add-on that goes with the newest Katari release, picked the way the app picks it (MpvAddonReleaseSource):
 * the release names the JNI glue level and lowest versionCode it needs, and the newest libmpv release of
 * pa2x2/mpv-android whose load-order.json matches has the add-on. A newer libmpv release can need a newer Katari, so
 * the newest one isn't always it.
 */
export interface MpvAddon {
  /** The libmpv release, such as `2026-10-27-1`. */
  version: string;
  page: string;
  apks: { abi: string; url: string; bytes: number }[];
}

const RELEASES = "https://api.github.com/repos/pa2x2/mpv-android/releases?per_page=30";
const TAG_PREFIX = "libmpv-";
/** As many releases as the app reads before giving up. */
const MAX_RELEASES_READ = 10;

interface Requirement {
  format: number;
  glue: number;
  minVersionCode: number;
}

interface LoadOrder {
  format: number;
  versionCode: number;
  glue: number;
  loadOrder: Record<string, string[]>;
}

let addon: Promise<MpvAddon | undefined> | undefined;

/** Undefined when GitHub can't be reached or no add-on fits; the page then links the add-on's releases. */
export function mpvAddon(): Promise<MpvAddon | undefined> {
  addon ??= findMpvAddon().catch((error: unknown) => {
    console.warn(`The download page links the mpv add-on's releases instead of its APKs: ${error}`);
    return undefined;
  });
  return addon;
}

async function findMpvAddon(): Promise<MpvAddon | undefined> {
  const katari = await latestRelease();
  if (!katari?.mpvRequirement) throw new Error("the newest Katari release names no mpv add-on");
  const requirement = JSON.parse(await fetchFromGitHub(katari.mpvRequirement)) as Requirement;
  if (requirement.format !== 1) throw new Error(`${katari.tag}'s mpv requirement has format ${requirement.format}`);
  const releases = (JSON.parse(await fetchFromGitHub(RELEASES)) as GitHubRelease[])
    .filter((release) => !release.draft && !release.prerelease && release.tag_name.startsWith(TAG_PREFIX))
    .slice(0, MAX_RELEASES_READ);
  for (const release of releases) {
    const version = release.tag_name.slice(TAG_PREFIX.length);
    const loadOrderAsset = release.assets.find((asset) => asset.name === `libmpv-android-${version}-load-order.json`);
    if (!loadOrderAsset) continue;
    const loadOrder = JSON.parse(await fetchFromGitHub(loadOrderAsset.browser_download_url)) as LoadOrder;
    if (loadOrder.format !== 2 || loadOrder.glue !== requirement.glue || loadOrder.versionCode < requirement.minVersionCode) continue;
    // Only the processor types Katari itself is released for.
    const apks = RELEASED_ABIS.filter((abi) => abi in loadOrder.loadOrder).flatMap((abi) => {
      const apk = release.assets.find((asset) => asset.name === `katari-mpv-${abi}-${version}.apk`);
      return apk ? [{ abi, url: apk.browser_download_url, bytes: apk.size }] : [];
    });
    if (apks.length > 0) return { version, page: release.html_url, apks };
  }
  return undefined;
}
