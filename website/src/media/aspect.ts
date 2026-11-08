import type { ImageMetadata } from "astro";
import { type CaptureImage, isPlanned } from "./planned-captures.ts";

/**
 * An imported image's width over its height. Reading any field of an imported image marks its full-size original as
 * used, and the build then ships it beside the resized copies, so this reads the clone Astro's own Picture reads.
 */
export function aspectOf(image: CaptureImage): number {
  if (isPlanned(image)) return image.aspect;
  const { width, height } = (image as ImageMetadata & { clone?: ImageMetadata }).clone ?? image;
  return width / height;
}
