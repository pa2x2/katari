/**
 * Captures a page can show before the showcase takes them. A page names such a capture by its file in src/captures;
 * until the file is there, its frame holds the caption and file name instead, in the proportions of the device it is
 * shown on, and the build warns once for each, so the page can go live first and the capture fill in later without a
 * code change.
 */
import type { ImageMetadata } from "astro";

export interface PlannedCapture {
  /** The file the showcase will write into src/captures, such as `sync-page.png`. */
  file: string;
}

export type CaptureImage = ImageMetadata | PlannedCapture;

const captures = import.meta.glob<ImageMetadata>("../captures/*.png", { eager: true, import: "default" });
const warned = new Set<string>();

export function plannedCapture(file: string): CaptureImage {
  const image = captures[`../captures/${file}`];
  if (image) return image;
  if (!warned.has(file)) {
    warned.add(file);
    console.warn(`src/captures has no ${file} yet, so its frame shows the file name until the showcase captures it`);
  }
  return { file };
}

export function isPlanned(image: CaptureImage): image is PlannedCapture {
  return "file" in image;
}
