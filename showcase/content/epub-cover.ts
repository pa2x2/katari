import { posix } from "node:path";
import { XMLParser } from "fast-xml-parser";
import { readMember } from "./zip-archive.ts";

type Node = Record<string, unknown>;

/** The cover image of [epub] and its file extension, found the EPUB 3 way first and the EPUB 2 way second. */
export function extractCover(epub: string): { image: Buffer; extension: string } {
  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "", removeNSPrefix: true });
  const container = parser.parse(readMember(epub, "META-INF/container.xml").toString("utf8"));
  const packagePath = String(list(container.container.rootfiles.rootfile)[0]!["full-path"]);
  const packageDocument = parser.parse(readMember(epub, packagePath).toString("utf8")).package as Node;
  const items = list((packageDocument.manifest as Node).item);
  const coverMeta = list((packageDocument.metadata as Node).meta).find((meta) => meta.name === "cover");
  const cover =
    items.find((item) => String(item.properties ?? "").split(" ").includes("cover-image")) ??
    items.find((item) => coverMeta !== undefined && item.id === coverMeta.content);
  if (cover === undefined) throw new Error(`${epub} declares no cover image`);
  const href = posix.normalize(posix.join(posix.dirname(packagePath), String(cover.href)));
  return { image: readMember(epub, href), extension: posix.extname(href) };
}

function list(node: unknown): Node[] {
  if (node === undefined) return [];
  return (Array.isArray(node) ? node : [node]) as Node[];
}
