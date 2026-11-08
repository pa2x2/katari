/**
 * Captures a page can show before the showcase takes them. A page names such a capture by its file in src/captures;
 * until the file is there, its frame holds the caption and file name instead, and the build warns once for each, so the
 * page can go live first and the capture fill in later without a code change.
 */
import type { ImageMetadata } from "astro";

export interface PlannedCapture {
  /** The file the showcase will write into src/captures, such as `sync-page.png`. */
  file: string;
  /** Width over height of the capture to come, so its frame takes the same room. */
  aspect: number;
}

export type CaptureImage = ImageMetadata | PlannedCapture;

/** The app on a phone, as the showcase captures it. */
export const PHONE_ASPECT = 1080 / 2424;
/** A desktop window or browser page. */
export const WINDOW_ASPECT = 16 / 10;

const captures = import.meta.glob<ImageMetadata>("../captures/*.png", { eager: true, import: "default" });
const warned = new Set<string>();

export function plannedCapture(file: string, aspect: number): CaptureImage {
  const image = captures[`../captures/${file}`];
  if (image) return image;
  if (!warned.has(file)) {
    warned.add(file);
    console.warn(`src/captures has no ${file} yet, so its frame shows the file name until the showcase captures it`);
  }
  return { file, aspect };
}

export function isPlanned(image: CaptureImage): image is PlannedCapture {
  return "file" in image;
}
