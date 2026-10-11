/**
 * Content-addressed download cache shared by `pin.ts` and `fetch.ts`.
 *
 * Files are stored once under their sha256, so pinning and fetching the same URL downloads it once, and a file whose
 * upstream bytes changed is never mistaken for the pinned one.
 */
import { createHash } from "node:crypto";
import { createWriteStream, existsSync, mkdirSync, readFileSync, renameSync, statSync, unlinkSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import { Readable, Transform } from "node:stream";
import { pipeline } from "node:stream/promises";
import type { ReadableStream } from "node:stream/web";

// Wikimedia rejects requests without a descriptive agent.
const USER_AGENT = "katari-showcase/1 (+https://github.com/pa2x2/katari)";

export interface CachedBlob {
  path: string;
  sha256: string;
  size: number;
}

export function cacheDir(): string {
  const path = process.env.KATARI_SHOWCASE_CACHE ?? join(homedir(), ".cache", "katari-showcase");
  mkdirSync(join(path, "blobs"), { recursive: true });
  return path;
}

export async function readText(url: string): Promise<string> {
  const response = await request(url);
  return response.text();
}

export async function download(url: string): Promise<CachedBlob> {
  const response = await request(url);
  const root = cacheDir();
  const temporary = join(root, `download-${process.pid}-${Date.now()}`);
  const digest = createHash("sha256");
  let size = 0;
  const measure = new Transform({
    transform(chunk: Buffer, _encoding, callback) {
      digest.update(chunk);
      size += chunk.length;
      callback(null, chunk);
    },
  });
  try {
    await pipeline(Readable.fromWeb(response.body as ReadableStream), measure, createWriteStream(temporary));
  } catch (error) {
    unlinkSync(temporary);
    throw error;
  }
  const sha256 = digest.digest("hex");
  const path = join(root, "blobs", sha256);
  renameSync(temporary, path);
  remember(url, sha256);
  return { path, sha256, size };
}

/** The blob last downloaded from [url], if it is still cached. */
export function known(url: string): CachedBlob | undefined {
  const sha256 = readIndex()[url];
  if (sha256 === undefined) return undefined;
  const path = join(cacheDir(), "blobs", sha256);
  return existsSync(path) ? { path, sha256, size: statSync(path).size } : undefined;
}

/** The cached blob with [sha256], downloaded from [url] when missing. */
export async function ensure(url: string, sha256: string): Promise<string> {
  const path = join(cacheDir(), "blobs", sha256);
  if (existsSync(path)) return path;
  const downloaded = await download(url);
  if (downloaded.sha256 !== sha256) {
    throw new Error(
      `${url} no longer matches its pin: expected sha256 ${sha256}, got ${downloaded.sha256}.\n` +
        "The upstream file changed. Review it, then run the pin again to accept it.",
    );
  }
  return downloaded.path;
}

async function request(url: string): Promise<Response> {
  const response = await fetch(url, { headers: { "User-Agent": USER_AGENT } });
  if (!response.ok || response.body === null) throw new Error(`${url} answered ${response.status}`);
  return response;
}

function indexPath(): string {
  return join(cacheDir(), "urls.json");
}

function readIndex(): Record<string, string> {
  return existsSync(indexPath()) ? JSON.parse(readFileSync(indexPath(), "utf8")) : {};
}

function remember(url: string, sha256: string): void {
  const index = readIndex();
  index[url] = sha256;
  const temporary = `${indexPath()}.tmp`;
  writeFileSync(temporary, JSON.stringify(index, null, 1));
  renameSync(temporary, indexPath());
}
