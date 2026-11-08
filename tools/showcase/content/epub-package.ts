import { posix } from "node:path";
import { XMLParser } from "fast-xml-parser";

export type PackageNode = Record<string, unknown>;

/** The member every EPUB keeps at this path, naming where its package document is. */
export const EPUB_CONTAINER = "META-INF/container.xml";

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "", removeNSPrefix: true });

/** Where the package document sits in the archive, as the EPUB's [container] document names it. */
export function packagePath(container: Buffer): string {
  return String(list(parser.parse(container.toString("utf8")).container.rootfiles.rootfile)[0]!["full-path"]);
}

/** The OPF package document [opf], parsed. */
export function parsePackage(opf: Buffer): PackageNode {
  return parser.parse(opf.toString("utf8")).package as PackageNode;
}

/** The archive path of a manifest href, which is relative to the package document. */
export function memberPath(packagePath: string, href: string): string {
  return posix.normalize(posix.join(posix.dirname(packagePath), href));
}

export function list(node: unknown): PackageNode[] {
  if (node === undefined) return [];
  return (Array.isArray(node) ? node : [node]) as PackageNode[];
}
