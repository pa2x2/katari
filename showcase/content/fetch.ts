/**
 * Builds the folder the showcase content server serves, from the works in sources.ts as their upstream sites have them.
 *
 * The folder holds `catalog.json` (see catalog.ts), which lists every chapter's files, and `files/`, with every cover
 * and the files of the chapters sources.ts fetches, under the paths the catalog gives. A film's video stays upstream
 * and is listed in `streamed.json`, from which the content server streams what the player reads.
 */
import { copyFileSync, existsSync, linkSync, mkdirSync, readFileSync, rmSync, unlinkSync, writeFileSync } from "node:fs";
import { dirname, extname, join, resolve } from "node:path";
import { parseArgs } from "node:util";
import sharp from "sharp";
import type { Catalog, CatalogChapter, CatalogWork } from "./catalog.ts";
import { STREAMED_FILES } from "./content-server.ts";
import { cacheDir, cached } from "./download-cache.ts";
import { extractCover } from "./epub-cover.ts";
import type { ResolvedWork } from "./resolved-work.ts";
import { resolveWork } from "./resolvers/resolve-work.ts";
import type { FilmWork, WorkDefinition } from "./source-definitions.ts";
import { sources, works } from "./sources.ts";
import { prefetch, readStreamed, streamedSize } from "./streamed-file.ts";
import { cuePoints } from "./webm-cues.ts";
import { localZip, remoteZip } from "./zip-archive.ts";

const CONCURRENT_WORKS = 4;
// How far past what the player has read the content server streams, as far as the connection's buffers take it.
const SERVER_READ_AHEAD = 8 << 20;

const { values } = parseArgs({ options: { out: { type: "string", default: join(cacheDir(), "served") } } });
const out = resolve(values.out);

const sourceIds = new Set(sources.map((source) => source.id));
for (const work of works) {
  if (!sourceIds.has(work.source)) throw new Error(`${work.id} names unknown source ${work.source}`);
}
rmSync(join(out, "files"), { recursive: true, force: true });
mkdirSync(join(out, "files"), { recursive: true });

const streamed: Record<string, string> = {};
const catalogWorks = await mapConcurrently(works, async (work) => {
  console.error(`fetching ${work.id}`);
  return materialize(await resolveWork(work), work);
});
const catalog: Catalog = { sources, works: catalogWorks };
writeFileSync(join(out, "catalog.json"), `${JSON.stringify(catalog, null, 2)}\n`);
writeFileSync(join(out, STREAMED_FILES), `${JSON.stringify(streamed, null, 2)}\n`);
console.log(out);

async function materialize(work: ResolvedWork, definition: WorkDefinition): Promise<CatalogWork> {
  const fetched = new Set(definition.fetchChapters);
  for (const number of fetched) {
    if (!work.chapters.some((chapter) => chapter.number === number)) throw new Error(`${work.id} has no chapter ${number}`);
  }
  const chapters: CatalogChapter[] = [];
  for (const chapter of work.chapters) {
    const files = chapter.files.map((url, index) => `files/${work.id}/${chapter.number}/${fileName(work, index + 1, url)}`);
    if (fetched.has(chapter.number)) {
      const played = definition.kind === "film" ? definition.played : undefined;
      for (const [index, url] of chapter.files.entries()) await place(work, url, files[index]!, played);
    }
    chapters.push({ number: chapter.number, name: chapter.name, date: chapter.date, files });
  }
  const { chapters: _, cover: __, ...described } = work;
  const credit = "credit" in work.cover ? (work.cover.credit ?? null) : null;
  return { ...described, cover: await cover(work, fetched), coverCredit: credit, chapters };
}

function fileName(work: ResolvedWork, index: number, url: string): string {
  switch (work.type) {
    case "manga":
      return `${String(index).padStart(3, "0")}${extname(url)}`;
    case "book":
      return "book.epub";
    case "anime":
      return `video${extname(url)}`;
  }
}

async function place(work: ResolvedWork, url: string, path: string, played?: FilmWork["played"]): Promise<void> {
  if (work.type === "anime") {
    // Asked now so that a video the player could not seek in fails the fetch rather than the player scene.
    await streamedSize(url);
    if (played !== undefined) await prefetchPlayback(url, played);
    streamed[path] = url;
  } else {
    link(await cached(url), join(out, path));
  }
}

/**
 * Downloads what the player reads of the WebM at [url] to play [played]: its header and first cluster, which it reads on
 * opening the film, its index, the clusters from the cue at or before the start to the first cue past the end, and the
 * last cluster, where a film the seed finished opens. The first two stretches run on by what the content server streams
 * ahead of the player.
 */
async function prefetchPlayback(url: string, played: NonNullable<FilmWork["played"]>): Promise<void> {
  if (extname(url) !== ".webm") throw new Error(`${url}: only a WebM says where its played stretch is`);
  const size = await streamedSize(url);
  const cues = await cuePoints((start, end) => readStreamed(url, start, Math.min(end, size)));
  const first = cues.findLast((cue) => cue.seconds <= played.from) ?? cues[0]!;
  const after = cues.find((cue) => cue.seconds > played.to);
  await prefetch(url, 0, Math.min((cues[1]?.offset ?? size) + SERVER_READ_AHEAD, size));
  await prefetch(url, first.offset, Math.min((after?.offset ?? size) + SERVER_READ_AHEAD, size));
  await prefetch(url, cues.at(-1)!.offset, size);
}

async function cover(work: ResolvedWork, fetched: Set<number>): Promise<string> {
  if ("fromEpub" in work.cover) {
    const chapter = work.chapters[0]!;
    const epub = chapter.files[0]!;
    // A book whose chapter is not fetched gives up only the bytes of its cover.
    const zip = fetched.has(chapter.number) ? localZip(readFileSync(await cached(epub))) : await remoteZip(epub);
    const { image, extension } = await extractCover((member) => zip.read(member));
    const path = `files/${work.id}/cover${extension}`;
    mkdirSync(dirname(join(out, path)), { recursive: true });
    writeFileSync(join(out, path), image);
    return path;
  }
  const download = await cached(work.cover.url);
  const path = `files/${work.id}/cover${extname(work.cover.url)}`;
  if (work.cover.crop === undefined) link(download, join(out, path));
  else {
    mkdirSync(dirname(join(out, path)), { recursive: true });
    await sharp(download).extract(work.cover.crop).jpeg({ quality: 92 }).toFile(join(out, path));
  }
  return path;
}

/**
 * [transform] applied to every item, a few items at a time, in the items' order. Each work takes several requests of
 * about a second in turn, and the Gutenberg mirror is run by volunteers, so only a few are asked for at once.
 */
async function mapConcurrently<T, R>(items: T[], transform: (item: T) => Promise<R>): Promise<R[]> {
  const results: R[] = new Array(items.length);
  let next = 0;
  const worker = async () => {
    while (next < items.length) {
      const index = next++;
      results[index] = await transform(items[index]!);
    }
  };
  await Promise.all(Array.from({ length: CONCURRENT_WORKS }, worker));
  return results;
}

function link(source: string, target: string): void {
  mkdirSync(dirname(target), { recursive: true });
  if (existsSync(target)) unlinkSync(target);
  try {
    linkSync(source, target);
  } catch {
    copyFileSync(source, target);
  }
}
