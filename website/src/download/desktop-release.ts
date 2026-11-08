/**
 * The desktop app's release the site offers (see offeredRelease), read from GitHub while the site builds. The site
 * rebuilds when a release is published.
 */
import type { GitHubRelease } from "./github.ts";
import { offeredRelease } from "./published-releases.ts";
import type { ReleaseFile } from "./release-format.ts";

export interface DesktopRelease {
  /** Such as `0.1.0-alpha.1`. */
  version: string;
  publishedAt: Date;
  /** The release's page on GitHub, with its notes. */
  page: string;
  /** The x86_64 AppImage, the one file the desktop app is released as. */
  appImage: ReleaseFile;
}

const TAG_PREFIX = "desktop-v";

let release: Promise<DesktopRelease | undefined> | undefined;

/**
 * Undefined before the first desktop release, and when GitHub can't be reached, such as in an offline build: the pages
 * then leave the desktop app's parts out.
 */
export function desktopRelease(): Promise<DesktopRelease | undefined> {
  release ??= fetchDesktopRelease().catch((error: unknown) => {
    console.warn(`The pages leave the desktop app's download out: ${error}`);
    return undefined;
  });
  return release;
}

async function fetchDesktopRelease(): Promise<DesktopRelease | undefined> {
  const offered = await offeredRelease(TAG_PREFIX);
  return offered && toDesktopRelease(offered);
}

function toDesktopRelease(release: GitHubRelease): DesktopRelease {
  const version = release.tag_name.slice(TAG_PREFIX.length);
  // Named as release.yml names it.
  const name = `Katari-${version}-x86_64.AppImage`;
  const appImage = release.assets.find((asset) => asset.name === name);
  if (!appImage) throw new Error(`${release.tag_name} has no ${name}`);
  return {
    version,
    publishedAt: new Date(release.published_at),
    page: release.html_url,
    appImage: { url: appImage.browser_download_url, bytes: appImage.size },
  };
}
