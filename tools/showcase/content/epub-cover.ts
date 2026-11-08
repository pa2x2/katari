import { posix } from "node:path";
import { EPUB_CONTAINER, list, memberPath, type PackageNode, packagePath, parsePackage } from "./epub-package.ts";

/**
 * The cover image of the EPUB whose members [read] returns, and its file extension, found the EPUB 3 way first and the
 * EPUB 2 way second.
 */
export async function extractCover(
  read: (member: string) => Buffer | Promise<Buffer>,
): Promise<{ image: Buffer; extension: string }> {
  const path = packagePath(await read(EPUB_CONTAINER));
  const document = parsePackage(await read(path));
  const items = list((document.manifest as PackageNode).item);
  const coverMeta = list((document.metadata as PackageNode).meta).find((meta) => meta.name === "cover");
  const cover =
    items.find((item) => String(item.properties ?? "").split(" ").includes("cover-image")) ??
    items.find((item) => coverMeta !== undefined && item.id === coverMeta.content);
  if (cover === undefined) throw new Error("The EPUB declares no cover image");
  const href = memberPath(path, String(cover.href));
  return { image: await read(href), extension: posix.extname(href) };
}
