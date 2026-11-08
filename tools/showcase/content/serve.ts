/**
 * Serves the folder fetch.ts builds to the emulator, which reaches it at 10.0.2.2, and the files fetch.ts lists in its
 * `streamed.json` from their upstream sites, a range at a time (streamed-file.ts).
 *
 * Videos are only playable through range requests, which the player uses to seek and to read a container's index at
 * its end, so this answers single byte ranges as well as whole files.
 */
import { createReadStream, existsSync, readFileSync, statSync } from "node:fs";
import { createServer, type ServerResponse } from "node:http";
import { extname, join, normalize, relative, resolve, sep } from "node:path";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import { parseArgs } from "node:util";
import { CONTENT_SERVER_PORT, STREAMED_FILES } from "./content-server.ts";
import { cacheDir } from "./download-cache.ts";
import { streamedBytes, streamedSize } from "./streamed-file.ts";

const MEDIA_TYPES: Record<string, string> = {
  ".json": "application/json; charset=utf-8",
  ".jpg": "image/jpeg",
  ".png": "image/png",
  ".epub": "application/epub+zip",
  ".webm": "video/webm",
  ".mp4": "video/mp4",
};

const { values } = parseArgs({
  options: {
    root: { type: "string", default: join(cacheDir(), "served") },
    port: { type: "string", default: String(CONTENT_SERVER_PORT) },
  },
});
const root = resolve(values.root);
const streamedList = join(root, STREAMED_FILES);
const streamed: Record<string, string> = existsSync(streamedList) ? JSON.parse(readFileSync(streamedList, "utf8")) : {};

const server = createServer(async (request, response) => {
  let pathname: string;
  try {
    pathname = decodeURIComponent(new URL(request.url ?? "/", "http://showcase").pathname);
  } catch {
    response.writeHead(400).end();
    return;
  }
  const path = normalize(join(root, pathname));
  const inRoot = path.startsWith(root + sep);
  const upstream = inRoot ? streamed[relative(root, path)] : undefined;
  try {
    const size = upstream !== undefined ? await streamedSize(upstream) : inRoot ? fileSize(path) : undefined;
    if (size === undefined || (request.method !== "GET" && request.method !== "HEAD")) {
      // A scene opened something sources.ts does not have fetched, and the screenshot will show it missing.
      console.error(`content server: nothing to serve for ${pathname}; see fetchChapters in content/sources.ts`);
      response.writeHead(404).end();
      return;
    }
    await respond(request.method, request.headers.range, path, size, upstream, response);
  } catch (error) {
    console.error(`content server: ${pathname} failed: ${error}`);
    if (response.headersSent) response.destroy();
    else response.writeHead(502).end();
  }
});

server.listen(Number(values.port), "127.0.0.1", () => console.log(`serving ${root} on 127.0.0.1:${values.port}`));

async function respond(
  method: string,
  rangeHeader: string | undefined,
  path: string,
  size: number,
  upstream: string | undefined,
  response: ServerResponse,
): Promise<void> {
  const headers = { "Content-Type": MEDIA_TYPES[extname(path)] ?? "application/octet-stream", "Accept-Ranges": "bytes" };
  const range = byteRange(rangeHeader, size);
  if (range === "unsatisfiable") {
    response.writeHead(416, { "Content-Range": `bytes */${size}` }).end();
    return;
  }
  const { start, end } = range ?? { start: 0, end: size - 1 };
  response.writeHead(range ? 206 : 200, {
    ...headers,
    "Content-Length": end - start + 1,
    ...(range && { "Content-Range": `bytes ${start}-${end}/${size}` }),
  });
  if (method === "HEAD" || size === 0) {
    response.end();
    return;
  }
  // A player that seeks closes the connection it no longer reads, which stops the stream before it fetches more.
  const body =
    upstream !== undefined ? Readable.from(streamedBytes(upstream, start, end)) : createReadStream(path, { start, end });
  await pipeline(body, response).catch((error) => {
    if (error.code !== "ERR_STREAM_PREMATURE_CLOSE") throw error;
  });
}

function fileSize(path: string): number | undefined {
  try {
    const stats = statSync(path);
    return stats.isFile() ? stats.size : undefined;
  } catch {
    return undefined;
  }
}

/** The single range a `Range` header asks for; multipart ranges are not needed by any client here and get the whole file. */
function byteRange(header: string | undefined, size: number): { start: number; end: number } | "unsatisfiable" | undefined {
  const match = header?.match(/^bytes=(\d*)-(\d*)$/);
  if (!match || (match[1] === "" && match[2] === "")) return undefined;
  const [, first, last] = match;
  const start = first === "" ? Math.max(0, size - Number(last)) : Number(first);
  const end = first === "" || last === "" ? size - 1 : Math.min(Number(last), size - 1);
  return start > end || start >= size ? "unsatisfiable" : { start, end };
}
