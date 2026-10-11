/**
 * Writes the showcase seed: a Katari backup holding the profiles, libraries, and a year of activity from
 * library-plan.ts, built over the catalog fetch.ts produced. The pipeline restores it into a fresh install.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { parseArgs } from "node:util";
import { gzipSync } from "node:zlib";
import type { Catalog, CatalogWork } from "../content/catalog.ts";
import { cacheDir } from "../content/download-cache.ts";
import { encodeBackup } from "./backup-messages.ts";
import { profiles } from "./library-plan.ts";
import { profileBackup } from "./profile-backup.ts";
import { sourceId } from "./source-id.ts";

const { values } = parseArgs({
  options: {
    catalog: { type: "string", default: join(cacheDir(), "served", "catalog.json") },
    out: { type: "string", default: join(cacheDir(), "showcase.tachibk") },
  },
});

const catalog: Catalog = JSON.parse(readFileSync(values.catalog, "utf8"));
const works = new Map(catalog.works.map((work) => [work.id, work]));
const sources = new Map(catalog.sources.map((source) => [source.id, source]));
const sourceIdOf = (work: CatalogWork) => {
  const source = sources.get(work.source) ?? raise(`${work.id} names unknown source ${work.source}`);
  return sourceId(source.name, source.language);
};

const backup = encodeBackup({
  sources: catalog.sources.map((source) => ({ name: source.name, sourceId: sourceId(source.name, source.language) })),
  profiles: profiles.map((plan, index) =>
    profileBackup(plan, works, sourceIdOf, (path) => join(dirname(values.catalog), path), index + 1),
  ),
  activeProfileUuid: profiles[0]!.uuid,
});
// zlib leaves the gzip header without a timestamp, so the file is byte-identical on every run.
writeFileSync(values.out, gzipSync(backup, { level: 9 }));
console.log(values.out);

function raise(message: string): never {
  throw new Error(message);
}
