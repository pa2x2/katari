export const REPOSITORY = "https://github.com/pa2x2/katari";
export const RELEASES = `${REPOSITORY}/releases`;

/**
 * Spread onto a link that leaves the site, so it opens in a new tab and the visitor keeps their place here. A new
 * object each time: Astro writes the component's style scope into a spread object's class.
 */
export function leavesSite(): { target: "_blank"; rel: "noopener" } {
  return { target: "_blank", rel: "noopener" };
}

/** A page or file of this site, under the base path GitHub Pages serves it from. */
export function sitePath(path = ""): string {
  return `${import.meta.env.BASE_URL.replace(/\/$/, "")}/${path}`;
}
