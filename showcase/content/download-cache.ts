/**
 * Download cache shared by `fetch.ts`, the content server, and the capture pipeline.
 *
 * A URL, or a byte range of one, is downloaded once and kept under a name derived from it, so repeated runs on one
 * machine reuse it and CI restores it between runs. Upstream files are not pinned: a fresh cache picks up whatever an
 * upstream site serves at the same URL by then, and the capture run shows any visible difference in the screenshots it
 * proposes. Delete the cache folder to download everything again.
 */
import { createHash } from "node:crypto";
import { createWriteStream, existsSync, mkdirSync, readFileSync, renameSync, unlinkSync, writeFileSync } from "node:fs";
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
  mkdirSync(downloadsDir(path), { recursive: true });
  return path;
}

/** Everything downloaded, and the only part of the cache CI keeps between runs. */
export function downloadsDir(cache = cacheDir()): string {
  return join(cache, "downloads");
}

/** A key for [url] that is safe as a file name. */
export function urlKey(url: string): string {
  return createHash("sha256").update(url).digest("hex");
}

export async function readText(url: string): Promise<string> {
  return readFileSync(await cached(url), "utf8");
}

// Callers that ask for the same download at once share it, rather than each writing the same partial file.
const inFlight = new Map<string, Promise<void>>();

/** The cached download of [url], downloaded first when missing. */
export async function cached(url: string): Promise<string> {
  const path = join(downloadsDir(), urlKey(url));
  if (!existsSync(path)) await once(path, () => download(url, path));
  return path;
}

/**
 * The bytes of [url] that the `Range` header value [range] asks for, with the size of the whole file. Kept in the cache
 * like a whole download, so a range has to be asked for the same way on every run to be found there again.
 */
export async function cachedRange(url: string, range: string): Promise<{ bytes: Buffer; size: number }> {
  const path = join(downloadsDir(), urlKey(`${url}#${range}`));
  if (!existsSync(path)) {
    await once(path, async () => {
      const response = await request(url, { Range: range });
      const size = rangeResponseSize(url, response);
      writeAtomically(path, JSON.stringify({ size }), Buffer.from(await response.arrayBuffer()));
    });
  }
  const [meta, bytes] = splitAtomicFile(readFileSync(path));
  return { bytes, size: JSON.parse(meta).size };
}

function once(path: string, write: () => Promise<void>): Promise<void> {
  let pending = inFlight.get(path);
  if (pending === undefined) {
    pending = write().finally(() => inFlight.delete(path));
    inFlight.set(path, pending);
  }
  return pending;
}

/** Requests [url], retrying the failures that an upstream site recovers from on its own. */
async function request(url: string, headers: Record<string, string> = {}): Promise<Response> {
  for (let attempt = 1; ; attempt++) {
    let failure = "";
    let response: Response | undefined;
    try {
      response = await fetchWithResponseTimeout(url, headers);
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

/** The size of the whole file a `206 Partial Content` [response] is part of. */
function rangeResponseSize(url: string, response: Response): number {
  const size = response.headers.get("Content-Range")?.match(/\/(\d+)$/)?.[1];
  if (response.status !== 206 || size === undefined) throw new Error(`${url} answers no byte ranges`);
  return Number(size);
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
async function fetchWithResponseTimeout(url: string, headers: Record<string, string>): Promise<Response> {
  const controller = new AbortController();
  const timeout = setTimeout(
    () => controller.abort(new Error(`no response in ${RESPONSE_TIMEOUT_MS / 1000} s`)),
    RESPONSE_TIMEOUT_MS,
  );
  try {
    return await fetch(url, { headers: { "User-Agent": USER_AGENT, ...headers }, signal: controller.signal });
  } finally {
    clearTimeout(timeout);
  }
}

/** Writes [meta] and [bytes] as one file, moved into place whole like a download. */
function writeAtomically(path: string, meta: string, bytes: Buffer): void {
  const header = Buffer.from(`${meta}\n`);
  writeFileSync(`${path}.partial`, Buffer.concat([header, bytes]));
  renameSync(`${path}.partial`, path);
}

function splitAtomicFile(file: Buffer): [string, Buffer] {
  const newline = file.indexOf(0x0a);
  return [file.subarray(0, newline).toString("utf8"), file.subarray(newline + 1)];
}
