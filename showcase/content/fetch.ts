/**
 * Builds the folder the showcase content server serves, from the works in sources.ts as their upstream sites have them.
 *
 * The folder holds `catalog.json` (see catalog.ts) and `files/`, with every page, book, video, and cover under the path
 * the catalog gives for it.
 */
import { copyFileSync, existsSync, linkSync, mkdirSync, renameSync, rmSync, unlinkSync, writeFileSync } from "node:fs";
import { dirname, extname, join, resolve } from "node:path";
import { parseArgs } from "node:util";
import sharp from "sharp";
import type { Catalog, CatalogChapter, CatalogWork } from "./catalog.ts";
import { cacheDir, cached, urlKey } from "./download-cache.ts";
import { extractCover } from "./epub-cover.ts";
import type { RemoteFile, ResolvedWork } from "./resolved-work.ts";
import { resolveWork } from "./resolvers/resolve-work.ts";
import { sources, works } from "./sources.ts";
import { extractMember } from "./zip-archive.ts";

const { values } = parseArgs({ options: { out: { type: "string", default: join(cacheDir(), "served") } } });
const out = resolve(values.out);

const sourceIds = new Set(sources.map((source) => source.id));
for (const work of works) {
  if (!sourceIds.has(work.source)) throw new Error(`${work.id} names unknown source ${work.source}`);
}
rmSync(join(out, "files"), { recursive: true, force: true });
mkdirSync(join(out, "files"), { recursive: true });

const catalogWorks: CatalogWork[] = [];
for (const work of works) {
  console.error(`fetching ${work.id}`);
  catalogWorks.push(await materialize(await resolveWork(work)));
}
const catalog: Catalog = { sources, works: catalogWorks };
writeFileSync(join(out, "catalog.json"), `${JSON.stringify(catalog, null, 2)}\n`);
console.log(out);

async function materialize(work: ResolvedWork): Promise<CatalogWork> {
  const chapters: CatalogChapter[] = [];
  for (const chapter of work.chapters) {
    const directory = `files/${work.id}/${chapter.number}`;
    const files = [];
    for (const [index, file] of chapter.files.entries()) {
      files.push(await place(file, `${directory}/${fileName(work, index + 1, file)}`));
    }
    chapters.push({ number: chapter.number, name: chapter.name, date: chapter.date, files });
  }
  const { chapters: _, cover: __, ...described } = work;
  const credit = "credit" in work.cover ? (work.cover.credit ?? null) : null;
  return { ...described, cover: await cover(work, chapters), coverCredit: credit, chapters };
}

function fileName(work: ResolvedWork, index: number, file: RemoteFile): string {
  switch (work.type) {
    case "manga":
      return `${String(index).padStart(3, "0")}${extname(file.url)}`;
    case "book":
      return "book.epub";
    case "anime":
      return `video${extname(file.member ?? file.url)}`;
  }
}

async function place(file: RemoteFile, path: string): Promise<string> {
  const download = await cached(file.url);
  const target = join(out, path);
  mkdirSync(dirname(target), { recursive: true });
  link(file.member === undefined ? download : await extracted(download, file.url, file.member), target);
  return path;
}

/** [member] of the zip [archive] downloaded from [url], extracted once into the cache. */
async function extracted(archive: string, url: string, member: string): Promise<string> {
  const path = join(cacheDir(), "extracted", urlKey(url), member);
  if (!existsSync(path)) {
    mkdirSync(dirname(path), { recursive: true });
    await extractMember(archive, member, `${path}.partial`);
    renameSync(`${path}.partial`, path);
  }
  return path;
}

async function cover(work: ResolvedWork, chapters: CatalogChapter[]): Promise<string> {
  if ("fromEpub" in work.cover) {
    const { image, extension } = extractCover(join(out, chapters[0]!.files[0]!));
    const path = `files/${work.id}/cover${extension}`;
    writeFileSync(join(out, path), image);
    return path;
  }
  const download = await cached(work.cover.url);
  const path = `files/${work.id}/cover${extname(work.cover.url)}`;
  if (work.cover.crop === undefined) link(download, join(out, path));
  else await sharp(download).extract(work.cover.crop).jpeg({ quality: 92 }).toFile(join(out, path));
  return path;
}

function link(source: string, target: string): void {
  if (existsSync(target)) unlinkSync(target);
  try {
    linkSync(source, target);
  } catch {
    copyFileSync(source, target);
  }
}
