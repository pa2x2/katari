/**
 * Re-encodes a screen recording for the site. The device records at full resolution and a high bit rate so that its
 * frames stay sharp; a page shows it at phone size, where a tenth of the size looks the same.
 *
 * A scene that recorded its clip slowed down leaves the factor in `<name>.slowdown` beside it; the clip is sped up by
 * as much and loses the note.
 */
import { existsSync, readFileSync, renameSync, rmSync } from "node:fs";
import { run } from "./run-command.ts";

// The recorder writes a frame only when the screen changes, so its clips have no steady rate of their own.
const FRAME_RATE = 30;

export function encodeForWeb(recording: string): void {
  const encoded = recording.replace(/\.mp4$/, ".web.mp4");
  const note = recording.replace(/\.mp4$/, ".slowdown");
  const slowdown = existsSync(note) ? Number(readFileSync(note, "utf8").trim()) : 1;
  if (!Number.isInteger(slowdown) || slowdown < 1) throw new Error(`${note} holds no slowdown: ${slowdown}`);
  run("ffmpeg", [
    "-v", "error",
    "-y",
    "-i", recording,
    "-an",
    "-vf", `setpts=PTS/${slowdown},fps=${FRAME_RATE},scale=720:-2`,
    "-c:v", "libx264",
    "-preset", "slow",
    "-crf", "28",
    "-pix_fmt", "yuv420p",
    // Lets a browser start playing before the whole file has arrived.
    "-movflags", "+faststart",
    encoded,
  ]);
  renameSync(encoded, recording);
  rmSync(note, { force: true });
}
