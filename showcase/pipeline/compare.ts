/**
 * Decides which fresh captures replace the published ones, so a run that changed nothing visible publishes nothing.
 *
 * A still counts as changed when enough of its pixels moved by more than rendering noise. A clip can't be compared
 * that way, since its frames land at slightly different moments each run, so it follows the stills of the same feature:
 * the recap clip is replaced when a recap still changed. It is also replaced when its format changed, which only a
 * change to how clips are recorded or encoded moves. Credits change when their text does.
 *
 * With --apply, changed and new files are copied over the published folder, and files no capture produces any more are
 * deleted from it.
 */
import { copyFileSync, existsSync, readdirSync, readFileSync, rmSync } from "node:fs";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";
import sharp from "sharp";
import { run } from "./run-command.ts";

/** A channel may differ by this much from antialiasing and gradient dithering without the pixel counting as changed. */
const CHANNEL_TOLERANCE = 24;
/**
 * The share of changed pixels below which a still is the same picture: about 100 pixels of a phone screen. Repeated
 * runs differ by a few dozen at most, while a changed digit or word moves several hundred.
 */
const CHANGED_SHARE = 0.00004;
/** The files a capture run publishes: stills, clips, and credits. */
const PUBLISHED = /\.(png|mp4|json)$/;
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

const files = readdirSync(captured).filter((name) => PUBLISHED.test(name));
const changed: string[] = [];
for (const name of files.filter((name) => !name.endsWith(".mp4"))) {
  const before = join(published, name);
  const after = join(captured, name);
  if (!existsSync(before)) changed.push(name);
  else if (name.endsWith(".png") ? await differs(before, after) : !readFileSync(before).equals(readFileSync(after))) {
    changed.push(name);
  }
}
for (const [clip, prefix] of Object.entries(CLIP_STILLS)) {
  if (!files.includes(clip)) continue;
  const before = join(published, clip);
  if (!existsSync(before) || changed.some((still) => still.startsWith(prefix))) changed.push(clip);
  else if (format(before) !== format(join(captured, clip))) changed.push(clip);
}
const retired = existsSync(published) ? readdirSync(published).filter((name) => PUBLISHED.test(name) && !files.includes(name)) : [];

if (values.apply) {
  for (const name of changed) copyFileSync(join(captured, name), join(published, name));
  for (const name of retired) rmSync(join(published, name));
}
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

function format(clip: string): string {
  return run("ffprobe", [
    "-v", "error",
    "-select_streams", "v:0",
    "-show_entries", "stream=codec_name,width,height,r_frame_rate",
    "-of", "csv=p=0",
    clip,
  ]).trim();
}

async function pixels(path: string) {
  const { data, info } = await sharp(path).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  return { data, width: info.width, height: info.height };
}
