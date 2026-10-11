/**
 * What the home page tells, in order: three chapters of features, each with its copy and screenshots. The components
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
import player from "../captures/player.png";
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

export type Shot =
  | { kind: "still"; image: ImageMetadata; alt: string; caption: string; landscape?: boolean }
  | { kind: "compare"; before: ImageMetadata; after: ImageMetadata; beforeLabel: string; afterLabel: string; alt: string; caption: string }
  | { kind: "clip"; src: string; poster: ImageMetadata; label: string; caption: string };

export interface Feature {
  id: string;
  title: string;
  text: string;
  highlights: string[];
  /** The first shot is the one in front. */
  shots: Shot[];
  /** Showpieces span the page on a background of their own instead of sitting beside their text. */
  band?: "indigo" | "recap";
}

export interface Chapter {
  id: string;
  numeral: string;
  title: string;
  intro: string;
  features: Feature[];
}

export const chapters: Chapter[] = [
  {
    id: "collect",
    numeral: "一",
    title: "Collect",
    intro: "Everything you read and watch, in one place.",
    features: [
      {
        id: "library",
        title: "Comics, books, and films on one shelf",
        text: "Everything you follow shares one library, one updates feed, and one history.",
        highlights: [
          "A title published in several places or languages becomes one entry with every version inside",
          "New chapters, episodes, and books arrive in one feed",
        ],
        shots: [
          {
            kind: "still",
            image: mergedEntry,
            alt: "Pepper&Carrot as one entry holding its English and Japanese editions",
            caption: "Two languages, one entry",
          },
          { kind: "still", image: updates, alt: "New episodes listed in the updates feed", caption: "The updates feed" },
        ],
      },
      {
        id: "discover",
        title: "Find the next story",
        text: "Search every source at once, or follow a source's popular and latest titles as a feed.",
        highlights: ["One search across comics, books, and films", "Feeds of what's popular and what's new"],
        shots: [
          { kind: "still", image: feed, alt: "A feed of popular books", caption: "Popular books as a feed" },
          {
            kind: "still",
            image: globalSearch,
            alt: "One search finding comics in two languages, books, and a film",
            caption: "One search, every source",
          },
        ],
      },
      {
        id: "profiles",
        title: "Everyone gets their own library",
        text: "Profiles keep separate libraries, history, statistics, and settings.",
        highlights: ["For a shared tablet, or for keeping collections apart"],
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
    title: "Read & watch",
    intro: "A reader made for each format, in any language.",
    features: [
      {
        id: "readers",
        title: "A reader made for each format",
        text: "Books flow like books, comics turn page by page, and films play in a player with the controls you expect.",
        highlights: ["Book themes for day and night", "Books read aloud", "A video player with episodes at hand"],
        shots: [
          { kind: "still", image: bookPaper, alt: "A book in the paper theme", caption: "The paper theme" },
          { kind: "still", image: bookDusk, alt: "The same page in the dusk theme", caption: "The dusk theme" },
          { kind: "still", image: player, alt: "Sintel playing in the video player", caption: "Sintel in the player", landscape: true },
        ],
      },
      {
        id: "translation",
        title: "Read the original, understand every line",
        text: "Katari finds the text on a comic page and translates it in place, with text recognition and translation running on the phone.",
        highlights: [
          "Tap a speech bubble to see it beside the original and hear either read aloud",
          "Translate anything you type in the Translator",
        ],
        band: "indigo",
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
    title: "Look back",
    intro: "Where your time went, and what it added up to.",
    features: [
      {
        id: "statistics",
        title: "See where your time goes",
        text: "Katari keeps track of how long you read and watch.",
        highlights: ["A daily goal and a streak", "Every month and year at a glance", "How long a title took, and how long the rest should take"],
        shots: [
          { kind: "still", image: statistics, alt: "Reading statistics for the last 30 days, with the daily goal", caption: "The last 30 days" },
          { kind: "still", image: statisticsYear, alt: "A year of reading, month by month", caption: "A year, month by month" },
          { kind: "still", image: entryActivity, alt: "Time spent on one book", caption: "Time spent on one book" },
        ],
      },
      {
        id: "recap",
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
];
