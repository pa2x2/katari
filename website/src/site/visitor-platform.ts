/** The platform Katari is released for that the visitor's browser runs on, or undefined on any other. */
export function visitorPlatform(): "Android" | "Linux" | undefined {
  // Android's browsers name Linux too, since Android runs on its kernel.
  if (/Android/.test(navigator.userAgent)) return "Android";
  if (/Linux/.test(navigator.userAgent)) return "Linux";
  return undefined;
}
