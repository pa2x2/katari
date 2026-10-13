/**
 * The guide's pages in reading order, grouped as the index and every page's contents show them. A page's file is
 * src/pages/guide/<slug>.astro.
 */
export interface GuideEntry {
  slug: string;
  title: string;
  /** One line on the index. */
  summary: string;
  /** The Katari version the page was last checked against, so a stale page reads as stale. */
  checked: string;
}

export interface GuideGroup {
  title: string;
  pages: GuideEntry[];
}

export const guide: GuideGroup[] = [
  {
    title: "Start",
    pages: [
      { slug: "install", title: "Install", summary: "Which file to download, how Katari updates, and moving from Mihon.", checked: "1.15" },
      { slug: "first-steps", title: "First steps", summary: "Add an extension store, find a title, and start reading.", checked: "1.15" },
    ],
  },
];

export const guidePages: GuideEntry[] = guide.flatMap((group) => group.pages);

export function guideEntry(slug: string): { entry: GuideEntry; group: GuideGroup; previous?: GuideEntry; next?: GuideEntry } {
  const index = guidePages.findIndex((page) => page.slug === slug);
  if (index < 0) throw new Error(`The guide's contents have no page ${slug}`);
  const entry = guidePages[index]!;
  return {
    entry,
    group: guide.find((group) => group.pages.includes(entry))!,
    previous: guidePages[index - 1],
    next: guidePages[index + 1],
  };
}
