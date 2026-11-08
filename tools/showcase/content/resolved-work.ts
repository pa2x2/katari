/** A work from sources.ts as its resolver describes it: its metadata and where each of its files is downloaded from. */
import type { CoverDefinition, EntryType, License, PlayedStretch, VideoVersion } from "./source-definitions.ts";

export interface ResolvedChapter {
  number: number;
  name: string;
  /** ISO date the chapter was published. */
  date: string;
  /** Upstream URLs of a comic chapter's pages or a book's EPUB; an episode lists its videos as [versions] instead. */
  files: string[];
  versions?: VideoVersion[];
  /** Of the first of the [versions], which the player opens. */
  played?: PlayedStretch;
  summary?: string;
  /** Upstream URL of an image from the episode. */
  still?: string;
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
