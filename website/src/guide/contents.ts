import type { SvgComponent } from "astro/types";
import accountCircle from "./icons/account_circle.svg";
import autoStories from "./icons/auto_stories.svg";
import callMerge from "./icons/call_merge.svg";
import code from "./icons/code.svg";
import compareArrows from "./icons/compare_arrows.svg";
import download from "./icons/download.svg";
import editNote from "./icons/edit_note.svg";
import explore from "./icons/explore.svg";
import extension from "./icons/extension.svg";
import filterList from "./icons/filter_list.svg";
import folder from "./icons/folder.svg";
import help from "./icons/help.svg";
import playCircle from "./icons/play_circle.svg";
import publicIcon from "./icons/public.svg";
import queryStats from "./icons/query_stats.svg";
import storage from "./icons/storage.svg";
import subtitles from "./icons/subtitles.svg";
import sync from "./icons/sync.svg";
import translate from "./icons/translate.svg";
import tune from "./icons/tune.svg";
import update from "./icons/update.svg";

/**
 * The guide's pages in reading order, grouped as the index and every page's contents show them. A page's file is
 * src/pages/guide/<slug>.astro, or <slug>/index.astro for a page with pages under it.
 */
export interface GuideEntry {
  slug: string;
  title: string;
  /** One line on the index. */
  summary: string;
  /**
   * The Material Symbol the app shows for the feature, copied from icons/material-symbols. Pages for extension
   * developers describe no screen of the app, so they take the nearest symbol from the same set.
   */
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
        slug: "video",
        title: "Watching video",
        summary: "The built-in player and mpv, servers, skipping intros, and watching offline.",
        icon: playCircle,
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
        summary: "Cloudflare checks, missing extensions, video that won't play, stopped updates, failed installs.",
        icon: help,
      },
    ],
  },
  {
    title: "Writing extensions",
    pages: [
      {
        slug: "extensions",
        title: "How extensions work",
        summary: "What an extension is, what Katari checks before loading one, and SDK versions.",
        icon: extension,
      },
      {
        slug: "extensions/project",
        title: "Set up a project",
        summary: "Gradle files, the manifest, and version numbers.",
        icon: code,
      },
      {
        slug: "extensions/source",
        title: "Write a source",
        summary: "Lists, search, refreshing details and chapters, and merged entries.",
        icon: editNote,
      },
      {
        slug: "extensions/filters",
        title: "Filters",
        summary: "Search controls, saved presets, dates and options from the site.",
        icon: filterList,
      },
      {
        slug: "extensions/media",
        title: "Pages and books",
        summary: "What a source hands the reader when a chapter opens.",
        icon: autoStories,
      },
      {
        slug: "extensions/video",
        title: "Video",
        summary: "Streams, servers, subtitles, audio tracks and skip markers for the player.",
        icon: subtitles,
      },
      {
        slug: "extensions/capabilities",
        title: "Optional capabilities",
        summary: "Settings, related titles, opening links and more.",
        icon: tune,
      },
      {
        slug: "extensions/store",
        title: "Publish an extension store",
        summary: "Sign your extensions and list them where Katari can find them.",
        icon: publicIcon,
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
