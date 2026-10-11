/** One profile of the seed: its library entries with their history, and the settings the screenshots rely on. */
import type { CatalogWork } from "../content/catalog.ts";
import { EMULATOR_CONTENT_SERVER } from "../content/content-server.ts";
import type {
  BackupBookProgressState,
  BackupEntry,
  BackupPreference,
  EntryType,
  ProfileScopedBackup,
} from "./backup-messages.ts";
import { bookPosition } from "./book-position.ts";
import { newChapters, type ProfilePlan } from "./library-plan.ts";
import { chapterUrl, readingHistory, type WorkHistory } from "./reading-history.ts";
import { LIBRARY_UPDATED_AT, SHOWCASE_NOW, STATISTICS_EPOCH } from "./showcase-clock.ts";

const ENTRY_TYPES: Record<CatalogWork["type"], EntryType> = { manga: "MANGA", book: "BOOK", anime: "ANIME" };

export function profileBackup(
  plan: ProfilePlan,
  works: Map<string, CatalogWork>,
  sourceIdOf: (work: CatalogWork) => bigint,
  servedFile: (path: string) => string,
  seed: number,
): ProfileScopedBackup {
  const work = (id: string) => works.get(id) ?? raise(`${plan.name} plans for unknown work ${id}`);
  const histories = readingHistory(plan, (id) => work(id).chapters.map((chapter) => chapter.number), seed);
  // An episode read in one language counts as read in the others merged with it.
  const readInMerge = (id: string): Set<number> =>
    new Set(
      (plan.merges.find((merge) => merge.includes(id)) ?? [id]).flatMap((member) => [
        ...(histories.get(member)?.readChapters ?? []),
      ]),
    );
  return {
    profile: { uuid: plan.uuid, name: plan.name, colorSeed: plan.colorSeed, position: plan.position },
    categories: [],
    preferences: preferences(plan),
    entries: plan.library.map((id) => entry(work(id), histories.get(id), mergeOf(plan, id, works, sourceIdOf))),
  };

  function entry(catalogWork: CatalogWork, history: WorkHistory | undefined, merge: BackupEntry["merge"]): BackupEntry {
    // Added to the library the day before it was first opened.
    const dateAdded = Math.min(history?.firstActivity ?? SHOWCASE_NOW, SHOWCASE_NOW) - 86_400_000;
    const chapters = [...catalogWork.chapters].sort((a, b) => b.number - a.number);
    const read = readInMerge(catalogWork.id);
    return {
      source: sourceIdOf(catalogWork),
      url: `/${catalogWork.id}`,
      title: catalogWork.title,
      artist: catalogWork.artist ?? undefined,
      author: catalogWork.author,
      description: catalogWork.description,
      genres: catalogWork.genres,
      status: catalogWork.status === "ongoing" ? 1 : 2,
      thumbnailUrl: `${EMULATOR_CONTENT_SERVER}/${catalogWork.cover}`,
      dateAdded,
      chapters: chapters.map((chapter, sourceOrder) => {
        const fetchedAt = newChapters.find(
          (update) => update.work === catalogWork.id && update.chapter === chapter.number,
        )?.fetchedAt;
        return {
          url: chapterUrl(catalogWork.id, chapter.number),
          name: chapter.name,
          read: read.has(chapter.number),
          lastPageRead: history?.lastPageRead.get(chapter.number) ?? 0,
          dateFetch: fetchedAt ?? dateAdded,
          dateUpload: fetchedAt === undefined ? Date.parse(chapter.date) : fetchedAt - 5 * 3_600_000,
          chapterNumber: chapter.number,
          sourceOrder,
          lastModifiedAt: Math.floor((fetchedAt ?? dateAdded) / 1000),
        };
      }),
      categories: [],
      favorite: true,
      history: history?.history ?? [],
      lastModifiedAt: Math.floor(SHOWCASE_NOW / 1000),
      merge,
      playbackStates: history?.playbackStates ?? [],
      progressStates: history?.bookProgress ? [bookProgress(catalogWork, history.bookProgress)] : [],
      lastUpdate: LIBRARY_UPDATED_AT,
      activitySessions: history?.sessions ?? [],
      activityCompletions: history?.completions ?? [],
      statisticsEpoch: STATISTICS_EPOCH,
      type: ENTRY_TYPES[catalogWork.type],
    };
  }

  function bookProgress(
    catalogWork: CatalogWork,
    progress: NonNullable<WorkHistory["bookProgress"]>,
  ): BackupBookProgressState {
    const chapter = catalogWork.chapters.find((candidate) => candidate.number === progress.chapter)!;
    const position = bookPosition(servedFile(chapter.files[0]!), progress.progression);
    return {
      chapterUrl: chapterUrl(catalogWork.id, progress.chapter),
      // The showcase extension names a book's only resource after its work (showcase/extension media/ShowcaseMedia.kt).
      resourceKey: `${catalogWork.id}-publication`,
      resourceRevision: position.revision,
      progression: position.totalProgression,
      totalProgression: position.totalProgression,
      extensions: JSON.stringify({
        "app.katari.book.location": {
          resourceId: position.resourceId,
          resourceProgression: position.resourceProgression,
        },
      }),
      updatedAt: progress.updatedAt,
    };
  }
}

/** The merge an entry belongs to; the root is merged at position 0 into itself, as the app records it. */
function mergeOf(
  plan: ProfilePlan,
  id: string,
  works: Map<string, CatalogWork>,
  sourceIdOf: (work: CatalogWork) => bigint,
): BackupEntry["merge"] {
  const members = plan.merges.find((merge) => merge.includes(id));
  if (members === undefined) return undefined;
  const root = works.get(members[0]!)!;
  return {
    targetSource: sourceIdOf(root),
    targetUrl: `/${root.id}`,
    targetType: ENTRY_TYPES[root.type],
    position: members.indexOf(id),
  };
}

function preferences(plan: ProfilePlan): BackupPreference[] {
  const tabs = plan.homeTabs;
  return [
    { key: "pref_theme_mode_key", value: { type: "String", value: "DARK" } },
    // Japanese sources are hidden until their language is enabled.
    { key: "source_languages", value: { type: "StringSet", value: ["all", "en", "ja"] } },
    { key: "translation_engine", value: { type: "String", value: "offline-translator" } },
    { key: "translation_target_language", value: { type: "String", value: "en" } },
    // The reader otherwise covers the first page it opens with its tap zones.
    { key: "reader_navigation_overlay_new_user", value: { type: "Boolean", value: false } },
    { key: "statistics_daily_goal_minutes", value: { type: "Int", value: plan.dailyGoalMinutes } },
    ...(tabs === undefined
      ? []
      : ([
          { key: "home_screen_tabs", value: { type: "StringSet", value: tabs } },
          // The app appends the tabs left out, so naming the shown ones is enough to order them.
          { key: "home_screen_tab_order", value: { type: "String", value: tabs.join(",") } },
        ] satisfies BackupPreference[])),
  ];
}

function raise(message: string): never {
  throw new Error(message);
}
