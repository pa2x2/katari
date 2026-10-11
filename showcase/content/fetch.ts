/**
 * Builds the folder the showcase content server serves, from the files pinned in pinned/content.lock.json.
 *
 * The folder holds `catalog.json` (see catalog.ts) and `files/`, with every page, book, video, and cover under the path
 * the catalog gives for it. Every download is checked against its pinned sha256 first, so the folder is the same on
 * every machine or the build stops.
 */
import { copyFileSync, existsSync, linkSync, mkdirSync, readFileSync, renameSync, rmSync, unlinkSync, writeFileSync } from "node:fs";
import { dirname, extname, join, resolve } from "node:path";
import { parseArgs } from "node:util";
import sharp from "sharp";
import type { Catalog, CatalogChapter, CatalogWork } from "./catalog.ts";
import { cacheDir, ensure } from "./download-cache.ts";
import { extractCover } from "./epub-cover.ts";
import type { Lock, LockedWork, PinnedFile } from "./lock.ts";
import { extractMember } from "./zip-archive.ts";

const { values } = parseArgs({ options: { out: { type: "string", default: join(cacheDir(), "served") } } });
const out = resolve(values.out);

const lock: Lock = JSON.parse(readFileSync(new URL("pinned/content.lock.json", import.meta.url), "utf8"));
rmSync(join(out, "files"), { recursive: true, force: true });
mkdirSync(join(out, "files"), { recursive: true });

const works: CatalogWork[] = [];
for (const work of lock.works) works.push(await materialize(work));
const catalog: Catalog = { sources: lock.sources, works };
writeFileSync(join(out, "catalog.json"), `${JSON.stringify(catalog, null, 2)}\n`);
console.log(out);

async function materialize(work: LockedWork): Promise<CatalogWork> {
  console.error(`fetching ${work.id}`);
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

function fileName(work: LockedWork, index: number, file: PinnedFile): string {
  switch (work.type) {
    case "manga":
      return `${String(index).padStart(3, "0")}${extname(file.url)}`;
    case "book":
      return "book.epub";
    case "anime":
      return `video${extname(file.member ?? file.url)}`;
  }
}

async function place(file: PinnedFile, path: string): Promise<string> {
  const blob = await ensure(file.url, file.sha256);
  const target = join(out, path);
  mkdirSync(dirname(target), { recursive: true });
  link(file.member === undefined ? blob : await extracted(blob, file.sha256, file.member), target);
  return path;
}

/** [member] of the pinned zip [archive], extracted once into the cache. */
async function extracted(archive: string, sha256: string, member: string): Promise<string> {
  const path = join(cacheDir(), "extracted", sha256, member);
  if (!existsSync(path)) {
    mkdirSync(dirname(path), { recursive: true });
    await extractMember(archive, member, `${path}.partial`);
    renameSync(`${path}.partial`, path);
  }
  return path;
}

async function cover(work: LockedWork, chapters: CatalogChapter[]): Promise<string> {
  if ("fromEpub" in work.cover) {
    const { image, extension } = extractCover(join(out, chapters[0]!.files[0]!));
    const path = `files/${work.id}/cover${extension}`;
    writeFileSync(join(out, path), image);
    return path;
  }
  const blob = await ensure(work.cover.url, work.cover.sha256);
  const path = `files/${work.id}/cover${extname(work.cover.url)}`;
  if (work.cover.crop === undefined) link(blob, join(out, path));
  else await sharp(blob).extract(work.cover.crop).jpeg({ quality: 92 }).toFile(join(out, path));
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
