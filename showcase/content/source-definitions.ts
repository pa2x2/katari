export type EntryType = "manga" | "book" | "anime";

export interface License {
  name: string;
  url: string;
}

export interface SourceDefinition {
  id: string;
  name: string;
  /** A BCP 47 tag, or `all` for a source whose entries differ in language. */
  language: string;
  type: EntryType;
}

export interface CoverDefinition {
  url: string;
  /** The part of the image that becomes the cover; the rest is cut off. */
  crop?: { left: number; top: number; width: number; height: number };
  /** Shown when the image is not part of the work it covers. */
  credit?: string;
}

interface WorkBase {
  id: string;
  source: string;
}

export interface PepperCarrotWork extends WorkBase {
  kind: "peppercarrot";
  language: string;
  episodes: { first: number; last: number };
  title: string;
  description: string;
  genres: string[];
  cover: CoverDefinition;
}

export interface GutenbergWork extends WorkBase {
  kind: "gutenberg";
  book: number;
  /** Replaces the catalog record's title where Gutenberg's capitalization is off. */
  title?: string;
  /** Replaces the cover inside the EPUB. */
  cover?: CoverDefinition;
}

export interface FilmWork extends WorkBase {
  kind: "film";
  title: string;
  author: string;
  artist: string;
  released: string;
  description: string;
  genres: string[];
  license: License;
  attribution: string;
  /** `member` names the video inside the downloaded zip, when it is one. */
  video: { url: string; member?: string };
  cover: CoverDefinition;
}

export type WorkDefinition = PepperCarrotWork | GutenbergWork | FilmWork;
