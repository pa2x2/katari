import type { EntryType, License, SourceDefinition } from "./source-definitions.ts";

export interface PinnedFile {
  url: string;
  sha256: string;
  size: number;
  /** The file inside the downloaded zip that the work uses. */
  member?: string;
}

export type LockedCover =
  | (PinnedFile & { crop?: { left: number; top: number; width: number; height: number }; credit?: string })
  | { fromEpub: true };

export interface LockedChapter {
  number: number;
  name: string;
  /** ISO date the chapter was published. */
  date: string;
  files: PinnedFile[];
}

export interface LockedWork {
  id: string;
  source: string;
  type: EntryType;
  title: string;
  author: string;
  artist: string | null;
  description: string;
  genres: string[];
  status: "ongoing" | "completed";
  language: string;
  license: License;
  attribution: string;
  cover: LockedCover;
  chapters: LockedChapter[];
}

export interface Lock {
  sources: SourceDefinition[];
  works: LockedWork[];
}
