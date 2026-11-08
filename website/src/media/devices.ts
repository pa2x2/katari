/**
 * What a capture shows the app on. The device picks the frame the site draws around the capture (Shot.astro) and,
 * while the capture is still planned, the proportions its frame keeps. Captures hold only the screen: a window's title
 * bar and a browser's toolbar are drawn by the site, so every Linux desktop's own decorations stay out of them.
 */
export type Device = "phone" | "phone-landscape" | "window" | "picture-in-picture" | "browser";

/** Width over height of a capture on each device, as the showcase takes it. */
export const deviceAspect: Record<Device, number> = {
  phone: 1080 / 2424,
  "phone-landscape": 2424 / 1080,
  window: 16 / 10,
  "picture-in-picture": 16 / 9,
  browser: 16 / 10,
};

export const isUpright = (device: Device) => device === "phone";
