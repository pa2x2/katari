/**
 * `catalog.json`, the contract between `fetch.ts` and the showcase extension: the extension lists sources, entries,
 * and chapters from it and requests every file by the path given here, relative to the content server's root.
 */
import type { ResolvedWork } from "./resolved-work.ts";
import type { SourceDefinition } from "./source-definitions.ts";

export interface CatalogChapter {
  number: number;
  name: string;
  date: string;
  /** Pages in reading order for manga, or the single EPUB of a book; empty for anime, whose [versions] list its videos. */
  files: string[];
  versions?: CatalogVideoVersion[];
  summary?: string;
  /** An image of a moment of the episode. */
  still?: string;
}

export interface CatalogVideoVersion {
  server: string;
  quality: string;
  file: string;
}

export interface CatalogWork extends Omit<ResolvedWork, "cover" | "chapters"> {
  cover: string;
  coverCredit: string | null;
  chapters: CatalogChapter[];
}

export interface Catalog {
  sources: SourceDefinition[];
  works: CatalogWork[];
}
