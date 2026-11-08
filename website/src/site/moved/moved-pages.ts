/**
 * Addresses pages had before they moved, and where each page is now. GitHub Pages can't redirect, so every old address
 * stays a page that sends the visitor on: released apps opened the docs under /guide/, and the developer pages lived
 * under /docs/extensions/ until they got a section of their own. The list only ever grows, except that a page of a feature
 * Katari dropped leaves it along with the page.
 */
const userSlugs = [
  "install",
  "first-steps",
  "extension-compatibility",
  "translation",
  "video",
  "merged-entries",
  "profiles",
  "statistics",
  "library-updates",
  "backups-and-storage",
  "local-source",
  "source-migration",
  "troubleshooting",
];

const developerSlugs = ["project", "source", "filters", "media", "video", "capabilities", "store", "sdk-changelog", "upgrade/3.0"];

/** Old path → current path, both relative to the site and ending in a slash. */
export const movedPages: Record<string, string> = Object.fromEntries([
  ["guide/", "docs/"],
  ...userSlugs.map((slug) => [`guide/${slug}/`, `docs/${slug}/`]),
  ["guide/extensions/", "developers/"],
  ["docs/extensions/", "developers/"],
  ...developerSlugs.flatMap((slug) => [
    [`guide/extensions/${slug}/`, `developers/${slug}/`],
    [`docs/extensions/${slug}/`, `developers/${slug}/`],
  ]),
]);

/** The static paths of a catch-all route under `prefix`, each with the page it sends visitors to. */
export function movedPaths(prefix: string): { params: { slug: string | undefined }; props: { target: string } }[] {
  return Object.entries(movedPages)
    .filter(([from]) => from.startsWith(prefix))
    .map(([from, target]) => ({ params: { slug: from.slice(prefix.length, -1) || undefined }, props: { target } }));
}
