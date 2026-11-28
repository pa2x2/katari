/**
 * The Mihon backup the bring-over scenes open: the comics of a profile's library as Mihon would have kept them, with
 * their reading progress, categories, a tracker link, and settings of which some come over and some stay behind. Field
 * numbers are those Katari's converter reads (features/backup conversion/mihon/MihonBackupModels.kt, from Mihon's
 * app/src/main/java/eu/kanade/tachiyomi/data/backup/models at mihonapp/mihon f8e30a860860); categories, sources, history
 * and preferences share Katari's fields.
 */
import type { CatalogWork } from "../content/catalog.ts";
import { CONTENT_SERVER } from "../content/content-server.ts";
import { type BackupPreference, encodePreference } from "./backup-messages.ts";
import type { ProfilePlan } from "./library-plan.ts";
import { ProtoWriter } from "./protobuf-writer.ts";
import { chapterUrl, readingHistory } from "./reading-history.ts";
import { SHOWCASE_NOW } from "./showcase-clock.ts";

/** The name the devices get the backup under: Mihon's for its automatic backups, which the bring-over screen shows. */
export const MIHON_BACKUP_NAME = "app.mihon_2026-12-15_09-12.tachibk";

/** Mihon's categories, by the order entries name them with. */
const CATEGORIES = [
  { name: "Comics", order: 0, id: 1 },
  { name: "Learning Japanese", order: 1, id: 2 },
];
const CATEGORY_OF_LANGUAGE: Record<string, number[]> = { en: [0], ja: [0, 1] };

/** Mihon's id for AniList, which a tracker link names. */
const ANILIST = 2;

const PREFERENCES: BackupPreference[] = [
  // Settings Katari has too.
  { key: "pref_theme_mode_key", value: { type: "String", value: "DARK" } },
  { key: "source_languages", value: { type: "StringSet", value: ["all", "en", "ja"] } },
  { key: "pref_library_columns_portrait_key", value: { type: "Int", value: 3 } },
  { key: "display_unread_badge", value: { type: "Boolean", value: true } },
  { key: "pref_show_page_number_key", value: { type: "Boolean", value: true } },
  { key: "pref_keep_screen_on_key", value: { type: "Boolean", value: true } },
  { key: "crop_borders", value: { type: "Boolean", value: true } },
  { key: "download_new", value: { type: "Boolean", value: false } },
  // Settings Katari has nothing for: Mihon's renderer, its landscape columns, its consent, and a tracker's.
  { key: "pref_high_quality_renderer_key", value: { type: "Boolean", value: true } },
  { key: "pref_library_columns_landscape_key", value: { type: "Int", value: 5 } },
  { key: "analytics", value: { type: "Boolean", value: true } },
  { key: "pref_auto_update_manga_sync_key", value: { type: "Boolean", value: true } },
];

export function encodeMihonBackup(
  plan: ProfilePlan,
  works: Map<string, CatalogWork>,
  sources: { name: string; sourceId: bigint }[],
  sourceIdOf: (work: CatalogWork) => bigint,
  seed: number,
): Uint8Array {
  const comics = plan.library.map((id) => works.get(id)!).filter((work) => work.type === "manga");
  const histories = readingHistory(plan, (id) => works.get(id)!.chapters.map((chapter) => chapter.number), seed);
  const usedSources = new Set(comics.map(sourceIdOf));
  const writer = new ProtoWriter();
  for (const work of comics) {
    const history = histories.get(work.id);
    const sourceId = sourceIdOf(work);
    const dateAdded = Math.min(history?.firstActivity ?? SHOWCASE_NOW, SHOWCASE_NOW) - 86_400_000;
    const chapters = [...work.chapters].sort((a, b) => b.number - a.number);
    writer.message(1, work, (manga) => {
      manga
        .int(1, sourceId)
        .string(2, `/${work.id}`)
        .string(3, work.title)
        .string(4, work.artist ?? undefined)
        .string(5, work.author)
        .string(6, work.description)
        .strings(7, work.genres)
        .int(8, work.status === "ongoing" ? 1 : 2)
        .string(9, `${CONTENT_SERVER}/${work.cover}`)
        .int(13, dateAdded)
        .messages(16, chapters, (chapter, value) =>
          chapter
            .string(1, chapterUrl(work.id, value.number))
            .string(2, value.name)
            .bool(4, history?.readChapters.has(value.number) ?? false)
            .int(6, history?.lastPageRead.get(value.number) ?? 0)
            .int(7, dateAdded)
            .int(8, Date.parse(value.date))
            .float(9, value.number)
            .int(10, chapters.indexOf(value)),
        );
      for (const category of CATEGORY_OF_LANGUAGE[work.language] ?? []) manga.int(17, CATEGORIES[category]!.order);
      if (work.language === "en") manga.message(18, ANILIST, (tracking, syncId) => tracking.int(1, syncId));
      manga
        .bool(100, true)
        .messages(104, history?.history ?? [], (entry, value) =>
          entry.string(1, value.url).int(2, value.lastRead).int(3, value.readDuration),
        );
    });
  }
  return writer
    .messages(2, CATEGORIES, (category, value) => category.string(1, value.name).int(2, value.order).int(3, value.id))
    .messages(
      101,
      sources.filter((source) => usedSources.has(source.sourceId)),
      (source, value) => source.string(1, value.name).int(2, value.sourceId),
    )
    .messages(104, PREFERENCES, encodePreference)
    .finish();
}
