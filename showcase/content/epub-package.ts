import { posix } from "node:path";
import { XMLParser } from "fast-xml-parser";
import { readMember } from "./zip-archive.ts";

export type PackageNode = Record<string, unknown>;

/** The OPF package document of [epub], found through its container, and where it sits in the archive. */
export function readPackage(epub: string): { path: string; document: PackageNode } {
  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "", removeNSPrefix: true });
  const container = parser.parse(readMember(epub, "META-INF/container.xml").toString("utf8"));
  const path = String(list(container.container.rootfiles.rootfile)[0]!["full-path"]);
  return { path, document: parser.parse(readMember(epub, path).toString("utf8")).package as PackageNode };
}

/** The archive path of a manifest href, which is relative to the package document. */
export function memberPath(packagePath: string, href: string): string {
  return posix.normalize(posix.join(posix.dirname(packagePath), href));
}

export function list(node: unknown): PackageNode[] {
  if (node === undefined) return [];
  return (Array.isArray(node) ? node : [node]) as PackageNode[];
}
