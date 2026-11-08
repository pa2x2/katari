/** A work from sources.ts as its resolver describes it: its metadata and where each of its files is downloaded from. */
import type { CoverDefinition, EntryType, License } from "./source-definitions.ts";

export interface ResolvedChapter {
  number: number;
  name: string;
  /** ISO date the chapter was published. */
  date: string;
  /** Upstream URLs of the chapter's files. */
  files: string[];
}

/** Gutenberg EPUBs carry their own cover, which fetch.ts extracts when sources.ts names none. */
export type ResolvedCover = CoverDefinition | { fromEpub: true };

export interface ResolvedWork {
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
  cover: ResolvedCover;
  chapters: ResolvedChapter[];
}

/** What a resolver works out; sources.ts gives the rest. */
export type ResolvedContent = Omit<ResolvedWork, "id" | "source" | "cover">;
