/**
 * The sync server's release the site offers (see offeredRelease), read from GitHub while the site builds. Its image is
 * pushed when the release is published, and the site rebuilds after that, so the version named here can be pulled.
 */
import { offeredRelease } from "./published-releases.ts";

export interface SyncServerRelease {
  /** Such as `1.0.0-beta.1`. */
  version: string;
  publishedAt: Date;
  /** The release's page on GitHub, with its notes. */
  page: string;
}

const TAG_PREFIX = "sync-server-v";

let release: Promise<SyncServerRelease | undefined> | undefined;

/**
 * Undefined before the server's first release, and when GitHub can't be reached, such as in an offline build: the pages
 * then leave the server's version out.
 */
export function syncServerRelease(): Promise<SyncServerRelease | undefined> {
  release ??= offeredRelease(TAG_PREFIX)
    .then((offered) =>
      offered && {
        version: offered.tag_name.slice(TAG_PREFIX.length),
        publishedAt: new Date(offered.published_at),
        page: offered.html_url,
      },
    )
    .catch((error: unknown) => {
      console.warn(`The pages leave the sync server's version out: ${error}`);
      return undefined;
    });
  return release;
}
