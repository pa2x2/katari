import { posix } from "node:path";
import { list, memberPath, type PackageNode, readPackage } from "./epub-package.ts";
import { readMember } from "./zip-archive.ts";

/** The cover image of [epub] and its file extension, found the EPUB 3 way first and the EPUB 2 way second. */
export function extractCover(epub: string): { image: Buffer; extension: string } {
  const { path, document } = readPackage(epub);
  const items = list((document.manifest as PackageNode).item);
  const coverMeta = list((document.metadata as PackageNode).meta).find((meta) => meta.name === "cover");
  const cover =
    items.find((item) => String(item.properties ?? "").split(" ").includes("cover-image")) ??
    items.find((item) => coverMeta !== undefined && item.id === coverMeta.content);
  if (cover === undefined) throw new Error(`${epub} declares no cover image`);
  const href = memberPath(path, String(cover.href));
  return { image: readMember(epub, href), extension: posix.extname(href) };
}
