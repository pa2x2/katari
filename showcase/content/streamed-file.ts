/**
 * A remote file the content server passes on to the emulator in the byte ranges the player asks for, so of a film only
 * what a scene plays is downloaded. Ranges are fetched in aligned chunks that the download cache keeps, so a later run
 * that plays the same part needs no network.
 */
import { cachedRange } from "./download-cache.ts";

const CHUNK_SIZE = 1 << 20;

export async function streamedSize(url: string): Promise<number> {
  return (await chunk(url, 0)).size;
}

/** The bytes of [url] from [start] to [end] inclusive, fetched only as the consumer reads them. */
export async function* streamedBytes(url: string, start: number, end: number): AsyncGenerator<Buffer> {
  for (let index = Math.floor(start / CHUNK_SIZE); index * CHUNK_SIZE <= end; index++) {
    const { bytes } = await chunk(url, index);
    const chunkStart = index * CHUNK_SIZE;
    yield bytes.subarray(Math.max(start - chunkStart, 0), Math.min(end - chunkStart + 1, bytes.length));
  }
}

/** The bytes of [url] from [start] up to [end], read through the same chunks the content server streams. */
export async function readStreamed(url: string, start: number, end: number): Promise<Buffer> {
  const parts: Buffer[] = [];
  for await (const part of streamedBytes(url, start, end - 1)) parts.push(part);
  return Buffer.concat(parts);
}

function chunk(url: string, index: number): Promise<{ bytes: Buffer; size: number }> {
  return cachedRange(url, `bytes=${index * CHUNK_SIZE}-${(index + 1) * CHUNK_SIZE - 1}`);
}
