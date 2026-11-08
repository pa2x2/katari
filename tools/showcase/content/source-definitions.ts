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
  /**
   * Chapters whose files the capture run needs: those a scene opens, and books the seed saves a reading position in,
   * which it measures and identifies by the EPUB. The other chapters are listed with their files left upstream, and a
   * work is otherwise shown by its cover and metadata alone. A film's video is streamed rather than downloaded.
   */
  fetchChapters?: number[];
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

/** One version of a video, which the player's settings offer by its server and quality. */
export interface VideoVersion {
  server: string;
  quality: string;
  /** A video that answers byte ranges, since it is streamed to the player rather than downloaded. */
  url: string;
}

interface VideoWorkBase extends WorkBase {
  title: string;
  author: string;
  artist: string;
  description: string;
  genres: string[];
  license: License;
  attribution: string;
  cover: CoverDefinition;
}

/**
 * The stretch of a video a scene plays, in seconds, which fetch.ts downloads ahead of the run with the WebM's header and
 * ending, so the player waits on no upstream site during the scene. The rest is streamed only if asked for.
 */
export interface PlayedStretch {
  from: number;
  to: number;
}

export interface FilmWork extends VideoWorkBase {
  kind: "film";
  released: string;
  /** The film's versions; the player opens the first, so it is the only one a scene plays. */
  versions: VideoVersion[];
  played?: PlayedStretch;
}

export interface SeriesEpisode {
  name: string;
  released: string;
  summary: string;
  /** An image of a moment of the episode, which the episode list shows beside it. */
  still: string;
  versions: VideoVersion[];
}

export interface SeriesWork extends VideoWorkBase {
  kind: "series";
  /** Numbered from 1 in this order. */
  episodes: SeriesEpisode[];
}

export type WorkDefinition = PepperCarrotWork | GutenbergWork | FilmWork | SeriesWork;
