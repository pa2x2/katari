/**
 * Resolves sources.ts into pinned/content.lock.json.
 *
 * Run it after editing sources.ts, or with --refresh to check every upstream file again; a changed upstream file then
 * shows up as a changed sha256 in the lock's diff.
 */
import { writeFileSync } from "node:fs";
import { parseArgs } from "node:util";
import type { Lock, LockedCover, LockedWork } from "./lock.ts";
import { resolveFilm } from "./resolvers/film.ts";
import { resolveGutenberg } from "./resolvers/gutenberg.ts";
import { resolvePepperCarrot } from "./resolvers/peppercarrot.ts";
import { pinFile } from "./resolvers/pinned-file.ts";
import type { WorkDefinition } from "./source-definitions.ts";
import { sources, works } from "./sources.ts";

const { values } = parseArgs({ options: { refresh: { type: "boolean", default: false } } });
const refresh = values.refresh;

const sourceIds = new Set(sources.map((source) => source.id));
const locked: LockedWork[] = [];
for (const work of works) {
  if (!sourceIds.has(work.source)) throw new Error(`${work.id} names unknown source ${work.source}`);
  console.error(`pinning ${work.id}`);
  locked.push({ id: work.id, source: work.source, ...(await resolve(work)), cover: await cover(work) });
}

const lock: Lock = { sources, works: locked };
writeFileSync(new URL("pinned/content.lock.json", import.meta.url), `${JSON.stringify(lock, null, 2)}\n`);

function resolve(work: WorkDefinition) {
  switch (work.kind) {
    case "peppercarrot":
      return resolvePepperCarrot(work, refresh);
    case "gutenberg":
      return resolveGutenberg(work, refresh);
    case "film":
      return resolveFilm(work, refresh);
  }
}

async function cover(work: WorkDefinition): Promise<LockedCover> {
  // Gutenberg EPUBs carry their own cover; fetch.ts extracts it, so there is nothing else to pin.
  if (work.cover === undefined) return { fromEpub: true };
  const { url, crop, credit } = work.cover;
  return { ...(await pinFile(url, refresh)), ...(crop && { crop }), ...(credit && { credit }) };
}
