import accountCircle from "../guide/icons/account_circle.svg";
import callMerge from "../guide/icons/call_merge.svg";
import compareArrows from "../guide/icons/compare_arrows.svg";
import download from "../guide/icons/download.svg";
import explore from "../guide/icons/explore.svg";
import extension from "../guide/icons/extension.svg";
import folder from "../guide/icons/folder.svg";
import help from "../guide/icons/help.svg";
import playCircle from "../guide/icons/play_circle.svg";
import queryStats from "../guide/icons/query_stats.svg";
import storage from "../guide/icons/storage.svg";
import sync from "../guide/icons/sync.svg";
import translate from "../guide/icons/translate.svg";
import update from "../guide/icons/update.svg";
import type { Manual } from "../guide/contents.ts";

/** Help with setting up and using Katari, in reading order. The app links into these pages, so their slugs stay. */
export const docs: Manual = {
  id: "docs",
  name: "Docs",
  titleSuffix: "Katari docs",
  base: "docs/",
  links: [{ label: "Writing an extension? Developers", path: "developers/" }],
  groups: [
    {
      title: "Get started",
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
      title: "Library",
      pages: [
        {
          slug: "library-updates",
          title: "Library updates",
          summary: "When Katari checks for new chapters, and what Upcoming shows.",
          icon: update,
        },
        {
          slug: "merged-entries",
          title: "Merged entries",
          summary: "One entry for every edition of a title.",
          icon: callMerge,
        },
        {
          slug: "tracking",
          title: "Tracking",
          summary: "Send your progress to AniList, MyAnimeList and other trackers.",
          icon: sync,
        },
      ],
    },
    {
      title: "Reading and watching",
      pages: [
        {
          slug: "video",
          title: "Watching video",
          summary: "The built-in player and mpv, servers, skipping intros, and watching offline.",
          icon: playCircle,
        },
        {
          slug: "translation",
          title: "Translation",
          summary: "Translate comic pages and typed text, on the phone or with a server.",
          icon: translate,
        },
      ],
    },
    {
      title: "Extensions and sources",
      pages: [
        {
          slug: "extension-compatibility",
          title: "Extension compatibility",
          summary: "Which extensions Katari loads, and what a label under Not loaded means.",
          icon: extension,
        },
        {
          slug: "source-migration",
          title: "Moving to another source",
          summary: "Move titles to a new source and keep what you've read.",
          icon: compareArrows,
        },
        {
          slug: "local-source",
          title: "Local source",
          summary: "Read comics from your own files.",
          icon: folder,
        },
      ],
    },
    {
      title: "Profiles and your data",
      pages: [
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
        {
          slug: "backups-and-storage",
          title: "Backups and storage",
          summary: "What a backup keeps, where files go, and moving from Mihon.",
          icon: storage,
        },
      ],
    },
    {
      title: "Help",
      boxed: true,
      pages: [
        {
          slug: "troubleshooting",
          title: "Troubleshooting",
          summary: "Cloudflare checks, missing extensions, video that won't play, stopped updates, failed installs.",
          icon: help,
        },
      ],
    },
  ],
};
