/**
 * The guide's pages in reading order, grouped as the index and every page's contents show them. A page's file is
 * src/pages/guide/<slug>.astro.
 */
export interface GuideEntry {
  slug: string;
  title: string;
  /** One line on the index. */
  summary: string;
}

export interface GuideGroup {
  title: string;
  pages: GuideEntry[];
}

export const guide: GuideGroup[] = [
  {
    title: "Start",
    pages: [
      { slug: "install", title: "Install", summary: "Which file to download, how Katari updates, and moving from Mihon." },
      { slug: "first-steps", title: "First steps", summary: "Add an extension store, find a title, and start reading." },
    ],
  },
  {
    title: "Katari's features",
    pages: [
      { slug: "translation", title: "Translation", summary: "Translate comic pages and typed text, on the phone or with a server." },
      { slug: "merged-entries", title: "Merged entries", summary: "One entry for every edition of a title." },
      { slug: "profiles", title: "Profiles", summary: "Separate libraries, history and settings on one phone." },
      { slug: "statistics", title: "Statistics and recaps", summary: "Daily goal, streaks, and your month and year as a story." },
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
