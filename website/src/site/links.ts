export const REPOSITORY = "https://github.com/pa2x2/katari";
export const RELEASES = `${REPOSITORY}/releases`;

/** A page or file of this site, under the base path GitHub Pages serves it from. */
export function sitePath(path = ""): string {
  return `${import.meta.env.BASE_URL.replace(/\/$/, "")}/${path}`;
}
