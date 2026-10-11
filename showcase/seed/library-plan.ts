/**
 * Who reads what in the showcase: each profile's library and the stretches of time it spent on each title.
 * reading-history.ts turns the stints into sessions, completions, and progress.
 */

export interface Stint {
  work: string;
  /** First and last day, inclusive, as ISO dates. */
  from: string;
  to: string;
  /** Chapter numbers read in order over the stint; all of the work's chapters when absent. */
  chapters?: { first: number; last: number };
  /** Whether the last chapter is finished by the end; otherwise it is still in progress. */
  finishes: boolean;
  /** Page the reader stopped on in an unfinished manga chapter, counted from 0. */
  stoppedAtPage?: number;
  /** How far into an unfinished book the reader got, from 0 to 1. */
  stoppedAtProgress?: number;
  /**
   * Reads each chapter in one sitting of about this long, spread evenly over the stint, instead of sharing the day's
   * reading time with the other stints. Comics take minutes per chapter, where books take weeks.
   */
  minutesPerChapter?: number;
}

export interface ProfilePlan {
  uuid: string;
  name: string;
  /** The profile picker takes its tile's hue from this seed modulo 360. */
  colorSeed: number;
  position: number;
  library: string[];
  /** Entries shown as one: the root first, then the entries merged into it. */
  merges: string[][];
  stints: Stint[];
  /** Days without any activity, inclusive. */
  breaks: { from: string; to: string }[];
  /** Days before today that each reach the daily goal, ending yesterday. */
  goalStreakDays: number;
  dailyGoalMinutes: number;
  /** Minutes already spent today when the screenshots are taken. */
  minutesToday: number;
  /** Tabs of the bottom bar in order, when they differ from the app's default. */
  homeTabs?: string[];
}

/**
 * Chapters that arrived after their entry was added, newest last. Updates lists only chapters uploaded in the last
 * three months, so these are dated as just released, a few hours before they were fetched, instead of on the catalog
 * date.
 */
export const newChapters: { work: string; chapter: number; fetchedAt: number }[] = [
  { work: "pepper-carrot-en", chapter: 7, fetchedAt: Date.UTC(2026, 11, 13, 8, 0) },
  { work: "pepper-carrot-ja", chapter: 7, fetchedAt: Date.UTC(2026, 11, 13, 8, 0) },
  { work: "pepper-carrot-en", chapter: 8, fetchedAt: Date.UTC(2026, 11, 15, 7, 0) },
  { work: "pepper-carrot-ja", chapter: 8, fetchedAt: Date.UTC(2026, 11, 15, 7, 0) },
];

/** Playback length of the films, in milliseconds. */
export const filmDurations: Record<string, number> = {
  sintel: 888_032,
  "big-buck-bunny": 634_600,
};

export const profiles: ProfilePlan[] = [
  {
    // The profile every installation starts with; restoring into it keeps a single default profile.
    uuid: "default",
    name: "Personal",
    colorSeed: 215,
    position: 0,
    library: [
      "pepper-carrot-en",
      "pepper-carrot-ja",
      "sherlock-holmes",
      "rashomon",
      "sintel",
      "pride-and-prejudice",
      "frankenstein",
      "big-buck-bunny",
      "alice-in-wonderland",
      "the-time-machine",
    ],
    merges: [["pepper-carrot-en", "pepper-carrot-ja"]],
    stints: [
      { work: "frankenstein", from: "2025-12-02", to: "2026-02-05", finishes: true },
      { work: "pride-and-prejudice", from: "2026-02-07", to: "2026-05-10", finishes: true },
      { work: "the-time-machine", from: "2026-05-12", to: "2026-05-19", finishes: true },
      // A summer of comics and films between books.
      {
        work: "pepper-carrot-en",
        from: "2026-06-01",
        to: "2026-06-28",
        chapters: { first: 1, last: 5 },
        finishes: true,
        minutesPerChapter: 25,
      },
      { work: "big-buck-bunny", from: "2026-06-20", to: "2026-06-20", finishes: true },
      { work: "alice-in-wonderland", from: "2026-07-01", to: "2026-07-07", finishes: true },
      { work: "sintel", from: "2026-07-30", to: "2026-07-30", finishes: true },
      // The Japanese original, a short story taken slowly.
      { work: "rashomon", from: "2026-08-01", to: "2026-08-06", finishes: true },
      {
        work: "sherlock-holmes",
        from: "2026-08-12",
        to: "2026-12-15",
        finishes: false,
        stoppedAtProgress: 0.62,
      },
      // Reads the episodes again in Japanese, slowly and with a dictionary.
      {
        work: "pepper-carrot-ja",
        from: "2026-10-01",
        to: "2026-12-15",
        chapters: { first: 1, last: 6 },
        finishes: false,
        stoppedAtPage: 1,
        minutesPerChapter: 60,
      },
    ],
    breaks: [{ from: "2026-07-18", to: "2026-07-26" }],
    goalStreakDays: 16,
    dailyGoalMinutes: 30,
    minutesToday: 22,
    // The language learner swaps History, which the showcase doesn't feature, for the Translator. A sixth tab would fold
    // More into an overflow menu.
    homeTabs: ["Library", "Updates", "Browse", "Translator", "More"],
  },
  {
    uuid: "4d2f6b1e-8c3a-4f7e-9b5d-2a1c7e9f3b60",
    name: "Family",
    colorSeed: 330,
    position: 1,
    library: ["big-buck-bunny", "alice-in-wonderland", "pepper-carrot-en"],
    merges: [],
    stints: [
      { work: "big-buck-bunny", from: "2026-10-03", to: "2026-10-03", finishes: true },
      {
        work: "pepper-carrot-en",
        from: "2026-10-20",
        to: "2026-11-15",
        chapters: { first: 1, last: 3 },
        finishes: true,
        minutesPerChapter: 12,
      },
      {
        work: "alice-in-wonderland",
        from: "2026-11-20",
        to: "2026-12-12",
        finishes: false,
        stoppedAtProgress: 0.55,
      },
    ],
    breaks: [],
    goalStreakDays: 0,
    dailyGoalMinutes: 0,
    minutesToday: 0,
  },
];
