import accountCircle from "../guide/icons/account_circle.svg";
import callMerge from "../guide/icons/call_merge.svg";
import chromeReaderMode from "../guide/icons/chrome_reader_mode.svg";
import compareArrows from "../guide/icons/compare_arrows.svg";
import download from "../guide/icons/download.svg";
import explore from "../guide/icons/explore.svg";
import extension from "../guide/icons/extension.svg";
import folder from "../guide/icons/folder.svg";
import help from "../guide/icons/help.svg";
import menuBook from "../guide/icons/menu_book.svg";
import playCircle from "../guide/icons/play_circle.svg";
import queryStats from "../guide/icons/query_stats.svg";
import settingsBackupRestore from "../guide/icons/settings_backup_restore.svg";
import storage from "../guide/icons/storage.svg";
import store from "../guide/icons/store.svg";
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
          title: "Install and updates",
          summary: "Which file to download, how Katari updates itself, and what it sends.",
          icon: download,
          related: ["docs/first-steps/", "docs/coming-from-mihon/", "docs/video/"],
        },
        {
          slug: "first-steps",
          title: "First steps",
          summary: "Add extensions, find a title, and start reading.",
          icon: explore,
          related: ["docs/extension-stores/", "docs/library-updates/", "docs/comic-reader/", "docs/book-reader/"],
        },
        {
          slug: "coming-from-mihon",
          title: "Coming from Mihon",
          summary: "Bring your library, history, settings and downloads over.",
          icon: settingsBackupRestore,
          related: ["docs/backups-and-storage/", "docs/extension-stores/", "docs/extension-compatibility/"],
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
          related: ["docs/tracking/", "docs/merged-entries/", "docs/troubleshooting/"],
        },
        {
          slug: "merged-entries",
          title: "Merged entries",
          summary: "One entry for every edition of a title.",
          icon: callMerge,
          related: ["docs/source-migration/", "docs/library-updates/", "docs/tracking/"],
        },
        {
          slug: "tracking",
          title: "Tracking",
          summary: "Send your progress to AniList, MyAnimeList and other trackers.",
          icon: sync,
          related: ["docs/profiles/", "docs/source-migration/", "docs/backups-and-storage/"],
        },
      ],
    },
    {
      title: "Reading and watching",
      pages: [
        {
          slug: "comic-reader",
          title: "Comic reader",
          summary: "Reading modes, turning pages, and fitting pages to the screen.",
          icon: chromeReaderMode,
          related: ["docs/translation/", "docs/book-reader/", "docs/statistics/"],
        },
        {
          slug: "book-reader",
          title: "Book reader",
          summary: "Themes, fonts, search, hiding repeated text, and listening.",
          icon: menuBook,
          related: ["docs/comic-reader/", "docs/translation/", "docs/statistics/"],
        },
        {
          slug: "video",
          title: "Watching video",
          summary: "The built-in player and mpv, servers, skipping intros, and watching offline.",
          icon: playCircle,
          related: ["docs/troubleshooting/", "docs/extension-compatibility/", "docs/statistics/"],
        },
        {
          slug: "translation",
          title: "Translation",
          summary: "Translate comic pages and typed text, on the phone or with a server.",
          icon: translate,
          related: ["docs/comic-reader/", "docs/book-reader/", "docs/backups-and-storage/"],
        },
      ],
    },
    {
      title: "Extensions and sources",
      pages: [
        {
          slug: "extension-stores",
          title: "Extension stores",
          summary: "Add a store, then install, update and trust extensions.",
          icon: store,
          related: ["docs/extension-compatibility/", "docs/troubleshooting/", "developers/store/"],
        },
        {
          slug: "extension-compatibility",
          title: "Extension compatibility",
          summary: "Which extensions Katari loads, and what a label under Not loaded means.",
          icon: extension,
          related: ["docs/extension-stores/", "docs/troubleshooting/", "developers/"],
        },
        {
          slug: "source-migration",
          title: "Moving to another source",
          summary: "Move titles to a new source and keep what you've read.",
          icon: compareArrows,
          related: ["docs/merged-entries/", "docs/tracking/", "docs/extension-stores/"],
        },
        {
          slug: "local-source",
          title: "Local source",
          summary: "Read comics from your own files.",
          icon: folder,
          related: ["docs/backups-and-storage/", "docs/comic-reader/"],
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
          related: ["docs/tracking/", "docs/backups-and-storage/", "docs/statistics/"],
        },
        {
          slug: "statistics",
          title: "Statistics and recaps",
          summary: "Daily goal, streaks, and your month and year as a story.",
          icon: queryStats,
          related: ["docs/profiles/", "docs/backups-and-storage/"],
        },
        {
          slug: "backups-and-storage",
          title: "Backups and storage",
          summary: "What a backup keeps, where files go, and restoring one.",
          icon: storage,
          related: ["docs/coming-from-mihon/", "docs/profiles/", "docs/local-source/"],
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
          related: ["docs/extension-compatibility/", "docs/library-updates/", "docs/video/", "docs/backups-and-storage/"],
        },
      ],
    },
  ],
};
