import type { WorkDefinition } from "../source-definitions.ts";
import type { ResolvedWork } from "../resolved-work.ts";
import { resolveFilm } from "./film.ts";
import { resolveGutenberg } from "./gutenberg.ts";
import { resolvePepperCarrot } from "./peppercarrot.ts";

/** A work's `kind` picks its resolver; every resolver reads the work's upstream site as it is now. */
export async function resolveWork(work: WorkDefinition): Promise<ResolvedWork> {
  const resolved = await (() => {
    switch (work.kind) {
      case "peppercarrot":
        return resolvePepperCarrot(work);
      case "gutenberg":
        return resolveGutenberg(work);
      case "film":
        return resolveFilm(work);
    }
  })();
  return { id: work.id, source: work.source, ...resolved, cover: work.cover ?? { fromEpub: true } };
}
