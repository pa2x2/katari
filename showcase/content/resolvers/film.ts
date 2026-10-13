/** Single films whose metadata is written in sources.ts; only their files are resolved. */
import type { ResolvedContent } from "../resolved-work.ts";
import type { FilmWork } from "../source-definitions.ts";

export function resolveFilm(work: FilmWork): ResolvedContent {
  return {
    type: "anime",
    title: work.title,
    author: work.author,
    artist: work.artist,
    description: work.description,
    genres: work.genres,
    status: "completed",
    language: "en",
    license: work.license,
    attribution: work.attribution,
    chapters: [
      {
        number: 1,
        name: work.title,
        date: work.released,
        files: [work.video.url],
      },
    ],
  };
}
