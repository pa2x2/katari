import type { ImageMetadata } from "astro";
import { type Device, deviceAspect } from "./devices.ts";
import { type CaptureImage, isPlanned } from "./planned-captures.ts";

/**
 * A capture's width over its height; a planned one takes its device's. Reading any field of an imported image marks its
 * full-size original as used, and the build then ships it beside the resized copies, so this reads the clone Astro's
 * own Picture reads.
 */
export function aspectOf(image: CaptureImage, device: Device): number {
  if (isPlanned(image)) return deviceAspect[device];
  const { width, height } = (image as ImageMetadata & { clone?: ImageMetadata }).clone ?? image;
  return width / height;
}
