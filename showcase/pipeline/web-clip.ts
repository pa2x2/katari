/**
 * Re-encodes a screen recording for the site. The device records at full resolution and a high bit rate so that its
 * frames stay sharp; a page shows it at phone size, where a tenth of the size looks the same.
 */
import { renameSync } from "node:fs";
import { run } from "./run-command.ts";

export function encodeForWeb(recording: string): void {
  const encoded = recording.replace(/\.mp4$/, ".web.mp4");
  run("ffmpeg", [
    "-v", "error",
    "-y",
    "-i", recording,
    "-an",
    "-vf", "scale=720:-2",
    "-c:v", "libx264",
    "-preset", "slow",
    "-crf", "28",
    "-pix_fmt", "yuv420p",
    // Lets a browser start playing before the whole file has arrived.
    "-movflags", "+faststart",
    encoded,
  ]);
  renameSync(encoded, recording);
}
