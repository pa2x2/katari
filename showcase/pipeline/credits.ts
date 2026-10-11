/**
 * `credits.json`, published beside the captures: the credit every work in the showcase library asks for, as the site's
 * credits page lists it. It comes from the catalog the captures were taken with, so it always matches the screenshots.
 */
import { readFileSync, writeFileSync } from "node:fs";
import type { Catalog } from "../content/catalog.ts";
import type { License } from "../content/source-definitions.ts";

export interface Credit {
  title: string;
  author: string;
  license: License;
  attribution: string;
  coverCredit: string | null;
}

export function writeCredits(catalogPath: string, creditsPath: string): void {
  const catalog: Catalog = JSON.parse(readFileSync(catalogPath, "utf8"));
  const credits: Credit[] = catalog.works
    .map(({ title, author, license, attribution, coverCredit }) => ({ title, author, license, attribution, coverCredit }))
    // A fixed locale, so every machine writes the same file and an unchanged run proposes nothing.
    .toSorted((a, b) => a.title.localeCompare(b.title, "en"));
  writeFileSync(creditsPath, `${JSON.stringify(credits, null, 2)}\n`);
}
