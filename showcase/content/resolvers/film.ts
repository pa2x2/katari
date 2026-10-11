/** Single films whose metadata is written in sources.ts; only their files are resolved. */
import type { LockedWork } from "../lock.ts";
import type { FilmWork } from "../source-definitions.ts";
import { pinFile } from "./pinned-file.ts";

export async function resolveFilm(work: FilmWork, refresh: boolean): Promise<Omit<LockedWork, "id" | "source" | "cover">> {
  const video = await pinFile(work.video.url, refresh);
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
        files: [work.video.member === undefined ? video : { ...video, member: work.video.member }],
      },
    ],
  };
}
