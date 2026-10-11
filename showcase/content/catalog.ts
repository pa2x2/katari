/**
 * `catalog.json`, the contract between `fetch.ts` and the showcase extension: the extension lists sources, entries,
 * and chapters from it and requests every file by the path given here, relative to the content server's root.
 */
import type { LockedWork } from "./lock.ts";
import type { SourceDefinition } from "./source-definitions.ts";

export interface CatalogChapter {
  number: number;
  name: string;
  date: string;
  /** Pages in reading order for manga; the single EPUB or video otherwise. */
  files: string[];
}

export interface CatalogWork extends Omit<LockedWork, "cover" | "chapters"> {
  cover: string;
  coverCredit: string | null;
  chapters: CatalogChapter[];
}

export interface Catalog {
  sources: SourceDefinition[];
  works: CatalogWork[];
}
