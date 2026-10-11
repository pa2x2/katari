import { download, known } from "../download-cache.ts";
import type { PinnedFile } from "../lock.ts";

/** A lock entry for [url]. Reuses the cached download unless [refresh] asks to check upstream again. */
export async function pinFile(url: string, refresh: boolean): Promise<PinnedFile> {
  const { sha256, size } = (refresh ? undefined : known(url)) ?? (await download(url));
  return { url, sha256, size };
}
