import type { ImageMetadata } from "astro";

/**
 * An imported image's width over its height. Reading any field of an imported image marks its full-size original as
 * used, and the build then ships it beside the resized copies, so this reads the clone Astro's own Picture reads.
 */
export function aspectOf(image: ImageMetadata): number {
  const { width, height } = (image as ImageMetadata & { clone?: ImageMetadata }).clone ?? image;
  return width / height;
}
