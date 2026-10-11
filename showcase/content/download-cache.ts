/**
 * Download cache shared by `fetch.ts` and the capture pipeline.
 *
 * A URL is downloaded once and kept under a name derived from it, so repeated runs on one machine reuse it. Upstream
 * files are not pinned: when one changes at the same URL, a fresh cache picks up the new file, and the capture run
 * shows any visible difference in the screenshots it proposes. Delete the cache folder to download everything again.
 */
import { createHash } from "node:crypto";
import { createWriteStream, existsSync, mkdirSync, renameSync, unlinkSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import type { ReadableStream } from "node:stream/web";

// Wikimedia rejects requests without a descriptive agent.
const USER_AGENT = "katari-showcase/1 (+https://github.com/pa2x2/katari)";

export function cacheDir(): string {
  const path = process.env.KATARI_SHOWCASE_CACHE ?? join(homedir(), ".cache", "katari-showcase");
  mkdirSync(join(path, "downloads"), { recursive: true });
  return path;
}

export async function readText(url: string): Promise<string> {
  const response = await request(url);
  return response.text();
}

/** A key for [url] that is safe as a file name. */
export function urlKey(url: string): string {
  return createHash("sha256").update(url).digest("hex");
}

/** The cached download of [url], downloaded first when missing. */
export async function cached(url: string): Promise<string> {
  const path = join(cacheDir(), "downloads", urlKey(url));
  if (existsSync(path)) return path;
  const response = await request(url);
  // Written beside the cache and moved in whole, so an interrupted download is never mistaken for a finished one.
  const partial = `${path}.partial`;
  try {
    await pipeline(Readable.fromWeb(response.body as ReadableStream), createWriteStream(partial));
  } catch (error) {
    if (existsSync(partial)) unlinkSync(partial);
    throw error;
  }
  renameSync(partial, path);
  return path;
}

async function request(url: string): Promise<Response> {
  const response = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!response.ok || response.body === null) throw new Error(`${url} answered ${response.status}`);
  return response;
}
