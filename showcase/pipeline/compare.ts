/**
 * Decides which fresh captures replace the published ones, so a run that changed nothing visible publishes nothing.
 *
 * A still counts as changed when enough of its pixels moved by more than rendering noise. A clip can't be compared
 * that way, since its frames land at slightly different moments each run, so it follows the stills of the same feature:
 * the recap clip is replaced when a recap still changed.
 *
 * With --apply, changed and new files are copied over the published folder; files no capture produces any more are
 * listed but left alone.
 */
import { copyFileSync, existsSync, readdirSync } from "node:fs";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";
import sharp from "sharp";

/** A channel may differ by this much from antialiasing and gradient dithering without the pixel counting as changed. */
const CHANNEL_TOLERANCE = 24;
/** The share of changed pixels below which a still is the same picture. */
const CHANGED_SHARE = 0.001;
/** Each clip and the prefix of the stills it follows. */
const CLIP_STILLS: Record<string, string> = { "recap.mp4": "recap-" };

const { values } = parseArgs({
  options: {
    published: { type: "string" },
    captured: { type: "string" },
    apply: { type: "boolean", default: false },
  },
});
if (values.published === undefined || values.captured === undefined) {
  throw new Error("Usage: compare.ts --published <folder> --captured <folder> [--apply]");
}
const published = resolve(values.published);
const captured = resolve(values.captured);

const files = readdirSync(captured).filter((name) => name.endsWith(".png") || name.endsWith(".mp4"));
const stills = files.filter((name) => name.endsWith(".png"));
const changed: string[] = [];
for (const still of stills) {
  if (!existsSync(join(published, still)) || (await differs(join(published, still), join(captured, still)))) {
    changed.push(still);
  }
}
for (const [clip, prefix] of Object.entries(CLIP_STILLS)) {
  if (!files.includes(clip)) continue;
  if (!existsSync(join(published, clip)) || changed.some((still) => still.startsWith(prefix))) changed.push(clip);
}
const retired = existsSync(published) ? readdirSync(published).filter((name) => /\.(png|mp4)$/.test(name) && !files.includes(name)) : [];

if (values.apply) for (const name of changed) copyFileSync(join(captured, name), join(published, name));
console.log(JSON.stringify({ changed, retired }, null, 2));

async function differs(before: string, after: string): Promise<boolean> {
  const [a, b] = await Promise.all([pixels(before), pixels(after)]);
  if (a.width !== b.width || a.height !== b.height) return true;
  let moved = 0;
  for (let offset = 0; offset < a.data.length; offset += 4) {
    if (
      Math.abs(a.data[offset]! - b.data[offset]!) > CHANNEL_TOLERANCE ||
      Math.abs(a.data[offset + 1]! - b.data[offset + 1]!) > CHANNEL_TOLERANCE ||
      Math.abs(a.data[offset + 2]! - b.data[offset + 2]!) > CHANNEL_TOLERANCE
    ) {
      moved++;
    }
  }
  return moved / (a.width * a.height) > CHANGED_SHARE;
}

async function pixels(path: string) {
  const { data, info } = await sharp(path).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  return { data, width: info.width, height: info.height };
}
