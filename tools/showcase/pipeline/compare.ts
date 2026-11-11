/**
 * Decides which fresh captures replace the published ones, so a run that changed nothing visible publishes nothing.
 *
 * A still counts as changed when its changed pixels cluster into a patch larger than rendering noise leaves. A pixel is
 * changed only when nothing within a pixel of its place in the other still comes close to it, so a page or a layout
 * that moved or stretched by up to a pixel, which no one looking at the still can see, changes nothing. A clip can't be
 * compared that way, since its frames land at slightly different moments each run, so it follows the stills of the
 * same feature: the recap clip is replaced when a recap still changed. It is also replaced when its format changed, which only a
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
 * The fewest touching changed pixels that make a still a different picture. Glyphs rasterised at another fraction of a
 * pixel and artwork resampled to a size a pixel apart leave clusters of up to about 15 pixels, while a changed digit
 * leaves one of 30 and more, in the phone's and the desktop's text alike.
 */
const CHANGED_CLUSTER = 20;
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
  const changed = new Uint8Array(a.width * a.height);
  // The outermost pixels are left out: a still shifted by a pixel shows a row or column there that the other one has
  // outside its frame.
  for (let y = 1; y < a.height - 1; y++) {
    for (let x = 1; x < a.width - 1; x++) {
      if (!matchesNear(a, b, x, y) || !matchesNear(b, a, x, y)) changed[y * a.width + x] = 1;
    }
  }
  return hasCluster(changed, a.width, CHANGED_CLUSTER);
}

/**
 * Whether each channel of [from]'s pixel at ([x], [y]) lies within [CHANNEL_TOLERANCE] of the range the pixels around
 * the same place in [to] span. A shift or stretch by less than a pixel blends neighbouring pixels, which keeps every
 * value between theirs.
 */
function matchesNear(from: Pixels, to: Pixels, x: number, y: number): boolean {
  for (let channel = 0; channel < 3; channel++) {
    const value = from.data[(y * from.width + x) * 4 + channel]!;
    let low = 255;
    let high = 0;
    for (let row = y - 1; row <= y + 1; row++) {
      for (let column = x - 1; column <= x + 1; column++) {
        const near = to.data[(row * to.width + column) * 4 + channel]!;
        if (near < low) low = near;
        if (near > high) high = near;
      }
    }
    if (value < low - CHANNEL_TOLERANCE || value > high + CHANNEL_TOLERANCE) return false;
  }
  return true;
}

/** Whether [changed] holds [size] or more set pixels touching one another, diagonally included. */
function hasCluster(changed: Uint8Array, width: number, size: number): boolean {
  const pending: number[] = [];
  for (let start = 0; start < changed.length; start++) {
    if (changed[start] !== 1) continue;
    changed[start] = 0;
    pending.push(start);
    let found = 0;
    while (pending.length > 0) {
      const index = pending.pop()!;
      if (++found >= size) return true;
      const x = index % width;
      for (let rowStart = index - x - width; rowStart <= index - x + width; rowStart += width) {
        for (let column = Math.max(x - 1, 0); column <= Math.min(x + 1, width - 1); column++) {
          const next = rowStart + column;
          if (changed[next] === 1) {
            changed[next] = 0;
            pending.push(next);
          }
        }
      }
    }
  }
  return false;
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

interface Pixels {
  data: Buffer;
  width: number;
  height: number;
}

async function pixels(path: string): Promise<Pixels> {
  const { data, info } = await sharp(path).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  return { data, width: info.width, height: info.height };
}
