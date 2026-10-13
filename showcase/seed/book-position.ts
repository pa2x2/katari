/**
 * A reading position inside an EPUB in the form Katari's book reader saves it: the spine document, how far into it,
 * and how far into the whole book. The reader validates the document against the publication and settles the exact
 * spot when it opens, so a position between paragraphs is enough.
 */
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { list, memberPath, type PackageNode, readPackage } from "../content/epub-package.ts";
import { readMember } from "../content/zip-archive.ts";

export interface BookPosition {
  /** The publication's sha256, which the reader checks positions against. */
  revision: string;
  resourceId: string;
  resourceProgression: number;
  totalProgression: number;
}

/** The position [progression] of the way through [epub], measured by the size of its spine documents. */
export function bookPosition(epub: string, progression: number): BookPosition {
  const { path, document } = readPackage(epub);
  const hrefs = new Map(list((document.manifest as PackageNode).item).map((item) => [item.id, String(item.href)]));
  const spine = list((document.spine as PackageNode).itemref).map((ref) => {
    const resourceId = memberPath(path, hrefs.get(ref.idref) ?? raise(`${epub} spine names unknown item ${ref.idref}`));
    return { resourceId, size: readMember(epub, resourceId).length };
  });
  const total = spine.reduce((sum, resource) => sum + resource.size, 0);
  let before = 0;
  for (const resource of spine) {
    if (before + resource.size >= progression * total) {
      const resourceProgression = (progression * total - before) / resource.size;
      return {
        revision: createHash("sha256").update(readFileSync(epub)).digest("hex"),
        resourceId: resource.resourceId,
        resourceProgression,
        totalProgression: (before + resourceProgression * resource.size) / total,
      };
    }
    before += resource.size;
  }
  throw new Error(`${epub} has an empty spine`);
}

function raise(message: string): never {
  throw new Error(message);
}
