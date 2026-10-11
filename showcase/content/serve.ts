/**
 * Serves the folder fetch.ts builds to the emulator, which reaches it at 10.0.2.2.
 *
 * Videos are only playable through range requests, which the player uses to seek and to read a container's index at
 * its end, so this answers single byte ranges as well as whole files.
 */
import { createReadStream, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize, resolve, sep } from "node:path";
import { parseArgs } from "node:util";
import { CONTENT_SERVER_PORT } from "./content-server.ts";
import { cacheDir } from "./download-cache.ts";

const MEDIA_TYPES: Record<string, string> = {
  ".json": "application/json; charset=utf-8",
  ".jpg": "image/jpeg",
  ".png": "image/png",
  ".epub": "application/epub+zip",
  ".mkv": "video/x-matroska",
  ".mp4": "video/mp4",
};

const { values } = parseArgs({
  options: {
    root: { type: "string", default: join(cacheDir(), "served") },
    port: { type: "string", default: String(CONTENT_SERVER_PORT) },
  },
});
const root = resolve(values.root);

const server = createServer((request, response) => {
  const path = normalize(join(root, decodeURIComponent(new URL(request.url ?? "/", "http://showcase").pathname)));
  const size = path.startsWith(root + sep) ? fileSize(path) : undefined;
  if (size === undefined || (request.method !== "GET" && request.method !== "HEAD")) {
    response.writeHead(404).end();
    return;
  }
  const headers = { "Content-Type": MEDIA_TYPES[extname(path)] ?? "application/octet-stream", "Accept-Ranges": "bytes" };
  const range = byteRange(request.headers.range, size);
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
  if (request.method === "HEAD" || size === 0) response.end();
  else createReadStream(path, { start, end }).pipe(response);
});

server.listen(Number(values.port), "127.0.0.1", () => console.log(`serving ${root} on 127.0.0.1:${values.port}`));

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
