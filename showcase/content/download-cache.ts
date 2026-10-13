/**
 * Download cache shared by `fetch.ts` and the capture pipeline.
 *
 * A URL is downloaded once and kept under a name derived from it, so repeated runs on one machine reuse it. Upstream
 * files are not pinned: when one changes at the same URL, a fresh cache picks up the new file, and the capture run
 * shows any visible difference in the screenshots it proposes. Delete the cache folder to download everything again.
 */
import { createHash } from "node:crypto";
import { createWriteStream, existsSync, mkdirSync, readFileSync, renameSync, unlinkSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import type { ReadableStream } from "node:stream/web";
import { setTimeout as sleep } from "node:timers/promises";

// Wikimedia rejects requests without a descriptive agent.
const USER_AGENT = "katari-showcase/1 (+https://github.com/pa2x2/katari)";
// Project Gutenberg's front end answers 503 and 504 for minutes at a time, and sometimes holds a request that long first.
const ATTEMPTS = 5;
const FIRST_RETRY_DELAY_MS = 5_000;
const RESPONSE_TIMEOUT_MS = 60_000;

export function cacheDir(): string {
  const path = process.env.KATARI_SHOWCASE_CACHE ?? join(homedir(), ".cache", "katari-showcase");
  mkdirSync(join(path, "downloads"), { recursive: true });
  return path;
}

/** A key for [url] that is safe as a file name. */
export function urlKey(url: string): string {
  return createHash("sha256").update(url).digest("hex");
}

export async function readText(url: string): Promise<string> {
  return readFileSync(await cached(url), "utf8");
}

/** The cached download of [url], downloaded first when missing. */
export async function cached(url: string): Promise<string> {
  const path = join(cacheDir(), "downloads", urlKey(url));
  if (!existsSync(path)) await download(url, path);
  return path;
}

/** Requests [url], retrying the failures that an upstream site recovers from on its own. */
async function request(url: string): Promise<Response> {
  for (let attempt = 1; ; attempt++) {
    let failure = "";
    let response: Response | undefined;
    try {
      response = await fetchWithResponseTimeout(url);
    } catch (error) {
      failure = `failed: ${error instanceof Error ? String(error.cause ?? error.message) : error}`;
    }
    if (response !== undefined) {
      if (response.ok && response.body !== null) return response;
      await response.body?.cancel();
      failure = `answered ${response.status}`;
      if (response.status < 500 && response.status !== 429) throw new Error(`${url} ${failure}`);
    }
    if (attempt === ATTEMPTS) throw new Error(`${url} ${failure}, ${ATTEMPTS} times`);
    const delay = FIRST_RETRY_DELAY_MS * 2 ** (attempt - 1);
    console.error(`  ${url} ${failure}; retrying in ${delay / 1000} s`);
    await sleep(delay);
  }
}

async function download(url: string, path: string): Promise<void> {
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
}

/** Only waits for the response to start; a large body may take longer than this to arrive. */
async function fetchWithResponseTimeout(url: string): Promise<Response> {
  const controller = new AbortController();
  const timeout = setTimeout(
    () => controller.abort(new Error(`no response in ${RESPONSE_TIMEOUT_MS / 1000} s`)),
    RESPONSE_TIMEOUT_MS,
  );
  try {
    return await fetch(url, { headers: { "User-Agent": USER_AGENT }, signal: controller.signal });
  } finally {
    clearTimeout(timeout);
  }
}
