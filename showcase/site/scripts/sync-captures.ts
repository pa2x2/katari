/**
 * Copies the stills and clips the site shows into src/captures, from a local capture run (showcase/pipeline/capture.ts)
 * by default, or from a checkout of the published assets.
 */
import { copyFileSync, mkdirSync, readdirSync, rmSync } from "node:fs";
import { homedir } from "node:os";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";

const { values } = parseArgs({
  options: {
    from: {
      type: "string",
      default: join(process.env.KATARI_SHOWCASE_CACHE ?? join(homedir(), ".cache", "katari-showcase"), "captures"),
    },
  },
});
const from = resolve(values.from);
const to = resolve(import.meta.dirname, "..", "src", "captures");

rmSync(to, { recursive: true, force: true });
mkdirSync(to, { recursive: true });
const files = readdirSync(from).filter((name) => /\.(png|mp4)$/.test(name));
for (const name of files) copyFileSync(join(from, name), join(to, name));
console.log(`${files.length} captures from ${from}`);
