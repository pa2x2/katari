import type { SvgComponent } from "astro/types";
import accountCircle from "./icons/account_circle.svg";
import callMerge from "./icons/call_merge.svg";
import compareArrows from "./icons/compare_arrows.svg";
import download from "./icons/download.svg";
import explore from "./icons/explore.svg";
import folder from "./icons/folder.svg";
import help from "./icons/help.svg";
import queryStats from "./icons/query_stats.svg";
import storage from "./icons/storage.svg";
import sync from "./icons/sync.svg";
import translate from "./icons/translate.svg";
import update from "./icons/update.svg";

/**
 * The guide's pages in reading order, grouped as the index and every page's contents show them. A page's file is
 * src/pages/guide/<slug>.astro.
 */
export interface GuideEntry {
  slug: string;
  title: string;
  /** One line on the index. */
  summary: string;
  /** The Material Symbol the app shows for the feature, copied from icons/material-symbols. */
  icon: SvgComponent;
}

export interface GuideGroup {
  title: string;
  pages: GuideEntry[];
}

export const guide: GuideGroup[] = [
  {
    title: "Start",
    pages: [
      {
        slug: "install",
        title: "Install",
        summary: "Which file to download, how Katari updates, and moving from Mihon.",
        icon: download,
      },
      {
        slug: "first-steps",
        title: "First steps",
        summary: "Add an extension store, find a title, and start reading.",
        icon: explore,
      },
    ],
  },
  {
    title: "Katari's features",
    pages: [
      {
        slug: "translation",
        title: "Translation",
        summary: "Translate comic pages and typed text, on the phone or with a server.",
        icon: translate,
      },
      {
        slug: "merged-entries",
        title: "Merged entries",
        summary: "One entry for every edition of a title.",
        icon: callMerge,
      },
      {
        slug: "profiles",
        title: "Profiles",
        summary: "Separate libraries, history and settings on one phone.",
        icon: accountCircle,
      },
      {
        slug: "statistics",
        title: "Statistics and recaps",
        summary: "Daily goal, streaks, and your month and year as a story.",
        icon: queryStats,
      },
    ],
  },
  {
    title: "Keeping things running",
    pages: [
      {
        slug: "library-updates",
        title: "Library updates",
        summary: "When Katari checks for new chapters, and what Upcoming shows.",
        icon: update,
      },
      {
        slug: "backups-and-storage",
        title: "Backups and storage",
        summary: "What a backup keeps, where files go, and moving from Mihon.",
        icon: storage,
      },
      {
        slug: "tracking",
        title: "Tracking",
        summary: "Send your progress to AniList, MyAnimeList and other trackers.",
        icon: sync,
      },
      {
        slug: "local-source",
        title: "Local source",
        summary: "Read comics from your own files.",
        icon: folder,
      },
      {
        slug: "source-migration",
        title: "Moving to another source",
        summary: "Move titles to a new source and keep what you've read.",
        icon: compareArrows,
      },
      {
        slug: "troubleshooting",
        title: "Troubleshooting",
        summary: "Cloudflare checks, missing extensions, stopped updates, failed installs.",
        icon: help,
      },
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
