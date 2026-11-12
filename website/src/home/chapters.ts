/**
 * What the home page tells, in order: four chapters of features, each with its copy, its screenshots and its size. The components
 * in this directory only lay it out.
 */
import type { ImageMetadata } from "astro";
import bookDusk from "../captures/book-dusk.png";
import bookPaper from "../captures/book-paper.png";
import entryActivity from "../captures/entry-activity.png";
import feed from "../captures/feed.png";
import globalSearch from "../captures/global-search.png";
import libraryFamily from "../captures/library-family.png";
import library from "../captures/library.png";
import mergedEntry from "../captures/merged-entry.png";
import profiles from "../captures/profiles.png";
import recapClip from "../captures/recap.mp4?url";
import recapShare from "../captures/recap-share.png";
import recapTop from "../captures/recap-top.png";
import statisticsYear from "../captures/statistics-year.png";
import statistics from "../captures/statistics.png";
import translationAfter from "../captures/translation-after.png";
import translationBefore from "../captures/translation-before.png";
import translationPopup from "../captures/translation-popup.png";
import translator from "../captures/translator.png";
import updates from "../captures/updates.png";
import type { Device } from "../media/devices.ts";
import { type CaptureImage, plannedCapture } from "../media/planned-captures.ts";

export type Shot =
  | { kind: "still"; image: CaptureImage; alt: string; caption: string; /** A phone when unset. */ device?: Exclude<Device, "browser"> }
  | { kind: "compare"; before: ImageMetadata; after: ImageMetadata; beforeLabel: string; afterLabel: string; alt: string; caption: string }
  | { kind: "clip"; src: string; poster: ImageMetadata; label: string; caption: string };

interface FeatureBase {
  id: string;
  title: string;
  text: string;
  /** The first shot is the one a full-size feature shows first. */
  shots: Shot[];
  /** Paths of the docs pages that explain the feature. */
  docs: string[];
}

/** A feature shown at full size: beside its shots, or across the page as a band. */
export interface FullFeature extends FeatureBase {
  highlights: string[];
  /** The feature in Katari for Linux. With these, a switch above the shots chooses between phone and computer. */
  computerShots?: Shot[];
  /** Showpieces span the page on a background of their own instead of sitting beside their text. */
  band?: "tinted" | "recap";
}

/** A feature shown as a card under its chapter's full-size ones: its upright shots, its title, one sentence and its docs. */
export interface CardFeature extends FeatureBase {
  card: true;
}

export type Feature = FullFeature | CardFeature;

export interface Chapter {
  id: string;
  numeral: string;
  /** The chapter's name in Japanese, set upright on its title page like the hero's 語り. */
  word: string;
  /** The two colours the title page's background runs between; dark, so the page reads the same in either theme. */
  tint: [string, string];
  title: string;
  intro: string;
  features: Feature[];
}

export const chapters: Chapter[] = [
  {
    id: "collect",
    numeral: "一",
    word: "集める",
    tint: ["#5a2a20", "#2a1611"],
    title: "Collect",
    intro: "Everything you read and watch, in one place.",
    features: [
      {
        id: "library",
        docs: ["docs/merged-entries/"],
        title: "Comics, books, and films on one shelf",
        text: "Everything you follow shares one library, one updates feed, and one history.",
        highlights: [
          "Merge a title published in several places or languages into one entry with every version inside",
          "New chapters, episodes, and books arrive in one feed",
        ],
        shots: [
          {
            kind: "still",
            image: mergedEntry,
            alt: "Pepper&Carrot as one entry holding its English and Japanese editions",
            caption: "Two editions, one entry",
          },
          { kind: "still", image: updates, alt: "New episodes listed in the updates feed", caption: "The updates feed" },
        ],
        computerShots: [
          {
            kind: "still",
            image: plannedCapture("sync-desktop.png"),
            alt: "A library holding comics, books, and films in Katari for Linux",
            caption: "The library in Katari for Linux",
            device: "window",
          },
        ],
      },
      {
        id: "discover",
        docs: ["docs/first-steps/"],
        title: "Find the next story",
        text: "Search every source at once, or follow a source's popular and latest titles as a feed.",
        card: true,
        shots: [
          { kind: "still", image: feed, alt: "A feed of popular books", caption: "Popular books as a feed" },
          {
            kind: "still",
            image: globalSearch,
            alt: "One search finding comics in two languages, books, and a film",
            caption: "Search every source",
          },
        ],
      },
      {
        id: "profiles",
        docs: ["docs/profiles/"],
        title: "Everyone gets their own library",
        text: "Profiles keep separate libraries, history, statistics, and reader and library settings.",
        card: true,
        shots: [
          { kind: "still", image: profiles, alt: "Choosing between two profiles", caption: "Pick a profile" },
          { kind: "still", image: library, alt: "The Personal profile's library", caption: "Personal" },
          { kind: "still", image: libraryFamily, alt: "The Family profile's library of children's classics", caption: "Family" },
        ],
      },
    ],
  },
  {
    id: "read-and-watch",
    numeral: "二",
    word: "読む・観る",
    tint: ["#1c4a44", "#0f2624"],
    title: "Read & watch",
    intro: "A reader made for each format, in any language.",
    features: [
      {
        id: "readers",
        docs: ["docs/comic-reader/", "docs/book-reader/", "docs/video/"],
        title: "A reader made for each format",
        text: "Books get a reader with themes and read-aloud, comics turn page by page, and films play in a full video player.",
        card: true,
        shots: [
          { kind: "still", image: plannedCapture("comic-reader.png"), alt: "A comic page in the reader", caption: "A comic in the reader" },
          { kind: "still", image: bookPaper, alt: "A book in the paper theme", caption: "The paper theme" },
          { kind: "still", image: bookDusk, alt: "The same page in the dusk theme", caption: "The dusk theme" },
        ],
      },
      {
        id: "translation",
        docs: ["docs/translation/"],
        title: "Read the original, understand every line",
        text: "Katari finds the text on a comic page and translates it in place. With an on-device engine, nothing leaves your phone.",
        highlights: [
          "Tap a speech bubble to see it beside the original and hear either read aloud",
          "Translate anything you type in the Translator",
        ],
        band: "tinted",
        shots: [
          {
            kind: "compare",
            before: translationBefore,
            after: translationAfter,
            beforeLabel: "Japanese",
            afterLabel: "English",
            alt: "A Japanese comic page with its speech bubbles translated into English",
            caption: "The same page in Japanese and in English",
          },
          {
            kind: "still",
            image: translationPopup,
            alt: "One speech bubble's translation, with buttons to hear it read aloud",
            caption: "One bubble, read aloud",
          },
          { kind: "still", image: translator, alt: "The Translator turning a line of Japanese into English", caption: "The Translator" },
        ],
      },
    ],
  },
  {
    id: "look-back",
    numeral: "三",
    word: "振り返る",
    tint: ["#1d3d66", "#0f1f38"],
    title: "Look back",
    intro: "Where your time went, and what it added up to.",
    features: [
      {
        id: "statistics",
        docs: ["docs/statistics/"],
        title: "See where your time goes",
        text: "Katari keeps track of how long you read and watch.",
        card: true,
        shots: [
          { kind: "still", image: statistics, alt: "Reading statistics for the last 30 days, with the daily goal", caption: "The last 30 days" },
          { kind: "still", image: statisticsYear, alt: "A year of reading, month by month", caption: "A year, month by month" },
          { kind: "still", image: entryActivity, alt: "Time spent on one book", caption: "Time spent on one book" },
        ],
      },
      {
        id: "recap",
        docs: ["docs/statistics/"],
        title: "Your year, as a story",
        text: "Every month and every year gets a recap that plays like a story.",
        highlights: ["The titles you spent the most time with, your longest streak, and the hour you read most", "Share it as a picture"],
        band: "recap",
        shots: [
          { kind: "clip", src: recapClip, poster: recapTop, label: "The year's recap playing as a story, page by page", caption: "The recap plays as a story" },
          { kind: "still", image: recapShare, alt: "The year's recap as a picture to share", caption: "Shared as a picture" },
        ],
      },
    ],
  },
  {
    id: "everywhere",
    numeral: "四",
    word: "どこでも",
    tint: ["#40295f", "#1d1530"],
    title: "Everywhere",
    intro: "One library on all your devices.",
    features: [
      {
        id: "desktop",
        docs: ["docs/install/", "docs/comic-reader/", "docs/book-reader/", "docs/video/"],
        title: "Katari on your computer",
        text: "The Linux app has the same library, readers and player, laid out for a wide window and at home with a mouse and keys.",
        highlights: [
          "More columns and bigger covers as the window grows",
          "Every reader and the player list their keys with ?",
          "Keep an episode playing in a small window while you work",
          "The desktop's media controls pause it and change episodes",
        ],
        shots: [
          {
            kind: "still",
            image: plannedCapture("desktop-book.png"),
            alt: "A book in Katari for Linux, two pages side by side on a wide window",
            caption: "A book two pages at a time",
            device: "window",
          },
          {
            kind: "still",
            image: plannedCapture("desktop-pip.png"),
            alt: "An episode playing in the small picture-in-picture window",
            caption: "An episode in a small window",
            device: "picture-in-picture",
          },
          {
            kind: "still",
            image: plannedCapture("desktop-keys.png"),
            alt: "The player listing every key it knows",
            caption: "Every key, one ? away",
            device: "window",
          },
        ],
      },
      {
        id: "sync",
        docs: ["docs/sync/", "docs/sync-server/"],
        title: "Your library on every device",
        text: "Your library, progress and history stay the same across your devices, through a server you run yourself.",
        highlights: [
          "Every device keeps its whole library and works offline",
          "Pick up on one device where you stopped on another",
          "Link a new device by scanning a code",
          "Choose what each device syncs, down to single settings and profiles",
        ],
        shots: [
          {
            kind: "still",
            image: plannedCapture("sync-desktop.png"),
            alt: "The same library in Katari for Linux",
            caption: "The same library on Linux",
            device: "window",
          },
          {
            kind: "still",
            image: plannedCapture("sync-page.png"),
            alt: "The Sync page listing the account's devices",
            caption: "The Sync page",
          },
          {
            kind: "still",
            image: plannedCapture("sync-link-code.png"),
            alt: "Link a device showing a code to scan",
            caption: "Link a device",
          },
          {
            kind: "still",
            image: plannedCapture("sync-handoff.png"),
            alt: "The reader offering to go where another device got to",
            caption: "Pick up where you stopped",
          },
        ],
      },
    ],
  },
];
