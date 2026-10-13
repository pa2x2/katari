/**
 * The parts of Katari's backup format the seed writes, with the field numbers of the app's `@ProtoNumber`s
 * (app/src/main/java/eu/kanade/tachiyomi/data/backup/models and mihon/feature/profiles/core/ProfileScopedBackup.kt).
 *
 * Restoring old backups is a compatibility guarantee of the app, so these numbers never change meaning; fields the
 * seed leaves out restore as their defaults.
 */
import { ProtoWriter } from "./protobuf-writer.ts";

export interface Backup {
  sources: { name: string; sourceId: bigint }[];
  profiles: ProfileScopedBackup[];
  activeProfileUuid: string;
}

export interface ProfileScopedBackup {
  profile: { uuid: string; name: string; colorSeed: number; position: number };
  categories: BackupCategory[];
  preferences: BackupPreference[];
  entries: BackupEntry[];
}

export interface BackupCategory {
  id: number;
  name: string;
  order: number;
}

export type PreferenceValue =
  | { type: "Int"; value: number }
  | { type: "Long"; value: number }
  | { type: "String"; value: string }
  | { type: "Boolean"; value: boolean }
  | { type: "StringSet"; value: string[] };

export interface BackupPreference {
  key: string;
  value: PreferenceValue;
}

export type EntryType = "MANGA" | "ANIME" | "BOOK";

export interface BackupEntry {
  source: bigint;
  url: string;
  title: string;
  artist?: string;
  author?: string;
  description?: string;
  genres: string[];
  /** SEntry status: 1 ongoing, 2 completed. */
  status: number;
  thumbnailUrl: string;
  /** Epoch milliseconds. */
  dateAdded: number;
  chapters: BackupChapter[];
  /** Category ids of the profile's categories. */
  categories: number[];
  favorite: boolean;
  history: BackupHistory[];
  /** Epoch seconds. */
  lastModifiedAt: number;
  merge?: { targetSource: bigint; targetUrl: string; targetType: EntryType; position: number };
  playbackStates: BackupPlaybackState[];
  progressStates: BackupBookProgressState[];
  /** Epoch milliseconds of the last library update that checked the entry. */
  lastUpdate: number;
  activitySessions: BackupActivitySession[];
  activityCompletions: BackupActivityCompletion[];
  /** Epoch milliseconds from which the entry's activity counts as precisely tracked. */
  statisticsEpoch: number;
  type: EntryType;
}

export interface BackupChapter {
  url: string;
  name: string;
  read: boolean;
  lastPageRead: number;
  /** Epoch milliseconds the app first saw the chapter; the Updates feed orders by it. */
  dateFetch: number;
  dateUpload: number;
  chapterNumber: number;
  sourceOrder: number;
  /** Epoch seconds. */
  lastModifiedAt: number;
}

export interface BackupHistory {
  url: string;
  lastRead: number;
  /** Milliseconds. */
  readDuration: number;
}

export interface BackupPlaybackState {
  url: string;
  positionMs: number;
  durationMs: number;
  completed: boolean;
  lastWatchedAt: number;
}

/**
 * A book reader position. The app's progress state also holds page and time positions, which the seed writes through
 * the chapter and playback fields instead.
 */
export interface BackupBookProgressState {
  chapterUrl: string;
  /** The publication resource id the source gives the book. */
  resourceKey: string;
  resourceRevision: string;
  progression: number;
  totalProgression: number;
  /** JSON object with the reader's precise location. */
  extensions: string;
  updatedAt: number;
}

export interface BackupActivitySession {
  sessionId: string;
  startedAt: number;
  endedAt: number;
  duration: number;
  segments: BackupActivitySegment[];
}

export interface BackupActivitySegment {
  chapterUrl: string;
  /** ISO date in [timeZoneId]. */
  localDate: string;
  timeZoneId: string;
  startedAt: number;
  endedAt: number;
  duration: number;
}

export interface BackupActivityCompletion {
  eventId: string;
  chapterUrl: string;
  sessionId: string;
  occurredAt: number;
  localDate: string;
  timeZoneId: string;
}

const ENTRY_TYPE_ORDINALS: Record<EntryType, number> = { MANGA: 0, ANIME: 1, BOOK: 2 };

export function encodeBackup(backup: Backup): Uint8Array {
  return new ProtoWriter()
    .messages(101, backup.sources, (writer, source) => writer.string(1, source.name).int(2, source.sourceId))
    .messages(200, backup.profiles, encodeProfile)
    .string(201, backup.activeProfileUuid)
    .finish();
}

function encodeProfile(writer: ProtoWriter, profile: ProfileScopedBackup): void {
  writer
    .message(1, profile.profile, (inner, value) =>
      inner.string(1, value.uuid).string(2, value.name).int(3, value.colorSeed).int(4, value.position).bool(5, false).bool(6, false),
    )
    .messages(2, profile.categories, (inner, category) =>
      inner.string(1, category.name).int(2, category.order).int(3, category.id),
    )
    .messages(4, profile.preferences, encodePreference)
    .messages(7, profile.entries, encodeEntry);
}

// kotlinx.serialization writes a sealed class as its serial name followed by the encoded subclass.
function encodePreference(writer: ProtoWriter, preference: BackupPreference): void {
  writer.string(1, preference.key).message(2, preference.value, (inner, value) => {
    inner.string(1, `eu.kanade.tachiyomi.data.backup.models.${value.type}PreferenceValue`).message(2, value, (field, typed) => {
      switch (typed.type) {
        case "Int":
        case "Long":
          field.int(1, typed.value);
          break;
        case "String":
          field.string(1, typed.value);
          break;
        case "Boolean":
          field.bool(1, typed.value);
          break;
        case "StringSet":
          field.strings(1, typed.value);
          break;
      }
    });
  });
}

function encodeEntry(writer: ProtoWriter, entry: BackupEntry): void {
  writer
    .int(1, entry.source)
    .string(2, entry.url)
    .string(3, entry.title)
    .string(4, entry.artist)
    .string(5, entry.author)
    .string(6, entry.description)
    .strings(7, entry.genres)
    .int(8, entry.status)
    .string(9, entry.thumbnailUrl)
    .int(10, entry.dateAdded)
    .messages(11, entry.chapters, encodeChapter);
  for (const category of entry.categories) writer.int(12, category);
  writer
    .bool(14, entry.favorite)
    .messages(17, entry.history, (inner, history) =>
      inner.string(1, history.url).int(2, history.lastRead).int(3, history.readDuration),
    )
    .int(19, entry.lastModifiedAt)
    .int(20, entry.lastModifiedAt)
    .int(22, 1)
    .bool(24, true);
  if (entry.merge) {
    writer.int(26, entry.merge.targetSource).string(27, entry.merge.targetUrl).int(28, entry.merge.position);
  }
  writer
    .string(29, "{}")
    .messages(30, entry.playbackStates, (inner, state) =>
      inner
        .string(1, state.url)
        .int(2, state.positionMs)
        .int(3, state.durationMs)
        .bool(4, state.completed)
        .int(5, state.lastWatchedAt),
    )
    .int(33, entry.lastUpdate)
    .messages(36, entry.progressStates, (inner, state) =>
      inner
        .string(2, state.resourceKey)
        .string(3, state.chapterUrl)
        .string(4, state.resourceRevision)
        .string(5, "book")
        .double(8, state.progression)
        .double(9, state.totalProgression)
        .string(10, state.extensions)
        .int(12, state.updatedAt),
    );
  if (entry.merge) writer.int(34, ENTRY_TYPE_ORDINALS[entry.merge.targetType]);
  if (entry.merge) {
    // The app reads merges from this feature state and keeps the fields above for older versions. Source ids exceed
    // JavaScript's safe integers, so the JSON is written by hand.
    const { targetSource, targetUrl, targetType, position } = entry.merge;
    const payload =
      `{"target":{"sourceId":${targetSource},"url":${JSON.stringify(targetUrl)},"type":"${targetType}"},` +
      `"position":${position}}`;
    writer.message(38, payload, (inner, value) => inner.string(1, "entry.merge.backup").int(2, 1).string(3, value));
  }
  writer
    .messages(40, entry.activitySessions, encodeSession)
    .messages(41, entry.activityCompletions, (inner, completion) =>
      inner
        .string(1, completion.eventId)
        .string(2, completion.chapterUrl)
        .string(3, completion.sessionId)
        .int(4, completion.occurredAt)
        .string(5, completion.localDate)
        .string(6, completion.timeZoneId)
        .string(7, "consumption"),
    )
    .int(42, entry.statisticsEpoch)
    .int(100, ENTRY_TYPE_ORDINALS[entry.type]);
}

function encodeChapter(writer: ProtoWriter, chapter: BackupChapter): void {
  writer
    .string(1, chapter.url)
    .string(2, chapter.name)
    .bool(4, chapter.read)
    .int(6, chapter.lastPageRead)
    .int(7, chapter.dateFetch)
    .int(8, chapter.dateUpload)
    .double(9, chapter.chapterNumber)
    .int(10, chapter.sourceOrder)
    .int(11, chapter.lastModifiedAt)
    .int(12, 1)
    .string(13, "{}");
}

function encodeSession(writer: ProtoWriter, session: BackupActivitySession): void {
  writer
    .string(1, session.sessionId)
    .int(2, session.startedAt)
    .int(3, session.endedAt)
    .int(4, session.duration)
    .int(5, session.segments.length)
    .messages(6, session.segments, (inner, segment) =>
      inner
        .string(1, segment.chapterUrl)
        .string(2, segment.localDate)
        .string(3, segment.timeZoneId)
        .int(4, segment.startedAt)
        .int(5, segment.endedAt)
        .int(6, segment.duration),
    );
}
