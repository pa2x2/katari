/** Project Gutenberg books, described by the RDF catalog record Gutenberg publishes beside every book. */
import { XMLParser } from "fast-xml-parser";
import { readText } from "../download-cache.ts";
import type { LockedWork } from "../lock.ts";
import type { GutenbergWork } from "../source-definitions.ts";
import { pinFile } from "./pinned-file.ts";

const GENERATED_SUMMARY_NOTE = "(This is an automatically generated summary.)";

type Node = Record<string, unknown>;

export async function resolveGutenberg(
  work: GutenbergWork,
  refresh: boolean,
): Promise<Omit<LockedWork, "id" | "source" | "cover">> {
  const record = await catalogRecord(work.book);
  const title = work.title ?? text(record.title).split("\n")[0]!.trim();
  const authors = list(record.creator).map((creator) => displayName(text((creator.agent as Node).name)));
  const genres = list(record.bookshelf)
    .map((shelf) => text((shelf.Description as Node).value))
    .filter((shelf) => shelf.startsWith("Category: "))
    .map((shelf) => shelf.slice("Category: ".length));
  return {
    type: "book",
    title,
    author: authors.join(", "),
    artist: null,
    description: text(record.marc520 ?? "").replace(GENERATED_SUMMARY_NOTE, "").trim(),
    genres,
    status: "completed",
    language: text(((record.language as Node).Description as Node).value),
    license: { name: "Public domain in the USA", url: "https://www.gutenberg.org/policy/license.html" },
    attribution: `Project Gutenberg eBook #${work.book}, www.gutenberg.org`,
    chapters: [
      {
        number: 1,
        name: title,
        date: text(record.issued),
        files: [await pinFile(`https://www.gutenberg.org/cache/epub/${work.book}/pg${work.book}-images-3.epub`, refresh)],
      },
    ],
  };
}

async function catalogRecord(book: number): Promise<Node> {
  const parser = new XMLParser({ ignoreAttributes: false, removeNSPrefix: true, parseTagValue: false });
  const document = parser.parse(await readText(`https://www.gutenberg.org/cache/epub/${book}/pg${book}.rdf`));
  return document.RDF.ebook as Node;
}

/** The text of an element the parser returned either as a string or, when it had attributes, as an object. */
function text(node: unknown): string {
  return typeof node === "object" && node !== null ? String((node as Node)["#text"]) : String(node);
}

function list(node: unknown): Node[] {
  if (node === undefined) return [];
  return (Array.isArray(node) ? node : [node]) as Node[];
}

/** Turns Gutenberg's "Carroll, Lewis" into "Lewis Carroll", dropping the expanded forms it adds in parentheses. */
function displayName(name: string): string {
  const [family, given] = name.split(" (")[0]!.split(", ");
  return given ? `${given} ${family}` : family!;
}
