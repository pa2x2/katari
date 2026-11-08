/** Films and series whose metadata is written in sources.ts; a film is one episode named after itself. */
import type { ResolvedContent } from "../resolved-work.ts";
import type { FilmWork, SeriesWork } from "../source-definitions.ts";

export function resolveFilm(work: FilmWork): ResolvedContent {
  return {
    ...videoWork(work),
    chapters: [
      { number: 1, name: work.title, date: work.released, files: [], versions: work.versions, played: work.played },
    ],
  };
}

export function resolveSeries(work: SeriesWork): ResolvedContent {
  return {
    ...videoWork(work),
    chapters: work.episodes.map((episode, index) => ({
      number: index + 1,
      name: episode.name,
      date: episode.released,
      files: [],
      versions: episode.versions,
      played: episode.played,
      summary: episode.summary,
      still: episode.still,
    })),
  };
}

function videoWork(work: FilmWork | SeriesWork): Omit<ResolvedContent, "chapters"> {
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
  };
}
