package eu.kanade.tachiyomi.data.backup.conversion.mihon

import app.katari.core.common.extensions.JsonObjectEmptyBytes
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/*
 * The Mihon family's backup, as far as converting it needs: field numbers from Mihon's
 * app/src/main/java/eu/kanade/tachiyomi/data/backup/models (mihonapp/mihon f8e30a860860), which TachiyomiSY, Komikku,
 * TachiyomiJ2K, Yōkai and Suwayomi keep for these fields. Fields not declared here are skipped; categories, sources,
 * history and preferences have the same fields as Katari's and decode into its models.
 */

/** The backup without its entries, which are read one at a time from field 1. */
@Serializable
internal class MihonBackup(
    @ProtoNumber(2) val categories: List<MihonCategory> = emptyList(),
    @ProtoNumber(101) val sources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) val preferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) val sourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) val extensionSources: List<MihonExtensionSource> = emptyList(),
    /** TachiyomiSY's and Komikku's. */
    @ProtoNumber(600) val savedSearches: List<MihonSavedSearch> = emptyList(),
    /** TachiyomiSY's (1.13.2 writes none). */
    @ProtoNumber(601) val syFeeds: List<MihonFeed> = emptyList(),
    /** Komikku's. */
    @ProtoNumber(610) val komikkuFeeds: List<MihonFeed> = emptyList(),
    /** Suwayomi's clients' settings, such as its web UI's. */
    @ProtoNumber(9000) val suwayomiMeta: Map<String, String> = emptyMap(),
    /** Suwayomi's server settings, read only for whether the backup holds them. */
    @ProtoNumber(9001) val suwayomiServerSettings: ByteArray? = null,
    @ProtoNumber(9002) val suwayomiUserSettings: ByteArray? = null,
)

@Serializable
internal class MihonCategory(
    @ProtoNumber(1) val name: String,
    /** The order categories are listed in, by which entries name their categories. */
    @ProtoNumber(2) val order: Long = 0,
    /** Absent in TachiyomiJ2K's and Yōkai's, which name categories by order alone. */
    @ProtoNumber(3) val id: Long = 0,
    @ProtoNumber(100) val flags: Long = 0,
    /** TachiyomiJ2K's and Yōkai's sort, as its `LibrarySort.categoryValue` encodes it. */
    @ProtoNumber(800) val j2kSort: Int? = null,
    /** Komikku's. */
    @ProtoNumber(900) val hidden: Boolean = false,
)

/** A search a person saved in a source: TachiyomiSY's and Komikku's. */
@Serializable
internal class MihonSavedSearch(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val query: String = "",
    /** The source's filters and their state, as TachiyomiSY's FilterSerializer writes them to JSON. */
    @ProtoNumber(3) val filterList: String = "",
    @ProtoNumber(4) val source: Long = 0,
)

/**
 * A feed: TachiyomiSY names the saved search it shows at 3, Komikku holds it at 9; neither means the source's latest
 * titles.
 */
@Serializable
internal class MihonFeed(
    @ProtoNumber(1) val source: Long = 0,
    /** On the Feed tab rather than only in the source's own page. */
    @ProtoNumber(2) val global: Boolean = true,
    @ProtoNumber(3) val savedSearchName: String? = null,
    @ProtoNumber(9) val savedSearch: MihonSavedSearch? = null,
)

@Serializable
internal class MihonManga(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(4) val artist: String? = null,
    @ProtoNumber(5) val author: String? = null,
    @ProtoNumber(6) val description: String? = null,
    @ProtoNumber(7) val genre: List<String> = emptyList(),
    @ProtoNumber(8) val status: Int = 0,
    @ProtoNumber(9) val thumbnailUrl: String? = null,
    @ProtoNumber(13) val dateAdded: Long = 0,
    /** The reading mode before [viewerFlags] replaced it. */
    @ProtoNumber(14) val viewer: Int = 0,
    @ProtoNumber(16) val chapters: List<MihonChapter> = emptyList(),
    /** The [MihonCategory.order] of each category the entry is in. */
    @ProtoNumber(17) val categories: List<Long> = emptyList(),
    @ProtoNumber(18) val tracking: List<MihonTracking> = emptyList(),
    /** Written only as false, for entries backed up for their reading state alone. */
    @ProtoNumber(100) val favorite: Boolean = true,
    @ProtoNumber(101) val chapterFlags: Int = 0,
    @ProtoNumber(103) val viewerFlags: Int? = null,
    @ProtoNumber(104) val history: List<BackupHistory> = emptyList(),
    @ProtoNumber(105) val updateStrategy: EntryUpdateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
    /** Seconds. */
    @ProtoNumber(107) val favoriteModifiedAt: Long? = null,
    @ProtoNumber(108) val excludedScanlators: List<String> = emptyList(),
    @ProtoNumber(110) val notes: String = "",
    @ProtoNumber(111) val initialized: Boolean = false,
    /** A JSON object the source keeps; TachiyomiJ2K declares it a string, which encodes the same. */
    @ProtoNumber(112) val memo: ByteArray = JsonObjectEmptyBytes,
    /** Epoch millis. */
    @ProtoNumber(113) val nextUpdate: Long = 0,
    /** On a TachiyomiSY or Komikku merged entry, its members and itself. */
    @ProtoNumber(600) val mergedEntries: List<MihonMergeReference> = emptyList(),
    /** TachiyomiSY's and Komikku's search metadata for sources such as E-Hentai; only that it's there is read. */
    @ProtoNumber(601) val flatMetadata: ByteArray? = null,
    @ProtoNumber(602) val customStatus: Int? = null,
    @ProtoNumber(603) val customThumbnailUrl: String? = null,
    @ProtoNumber(604) val mergeSettings: MihonMergeSettings? = null,
    @ProtoNumber(800) val customTitle: String? = null,
    @ProtoNumber(801) val customArtist: String? = null,
    @ProtoNumber(802) val customAuthor: String? = null,
    @ProtoNumber(804) val customDescription: String? = null,
    @ProtoNumber(805) val customGenre: List<String>? = null,
    /** What Suwayomi's clients kept for the entry. */
    @ProtoNumber(9000) val suwayomiMeta: Map<String, String> = emptyMap(),
)

/**
 * One title of a TachiyomiSY merged entry, and Komikku's same message, which adds a merge URL at 6 that isn't read.
 * The merged entry lists itself too, with the merged source's id.
 */
@Serializable
internal class MihonMergeReference(
    /** The title whose details the merged entry shows. */
    @ProtoNumber(1) val isInfoManga: Boolean = false,
    @ProtoNumber(2) val fetchChapterUpdates: Boolean = true,
    @ProtoNumber(3) val chapterSortMode: Int = 0,
    /** The title's place in the merged chapter list. */
    @ProtoNumber(4) val chapterPriority: Int = 0,
    @ProtoNumber(5) val downloadChapters: Boolean = true,
    @ProtoNumber(7) val mangaUrl: String = "",
    @ProtoNumber(8) val mangaSourceId: Long = 0,
)

/** TachiyomiSY's settings of a merged entry. */
@Serializable
internal class MihonMergeSettings(
    @ProtoNumber(1) val chapterSortMode: Int = 0,
    @ProtoNumber(2) val infoMangaUrl: String? = null,
    @ProtoNumber(3) val infoMangaSourceId: Long? = null,
)

@Serializable
internal class MihonChapter(
    @ProtoNumber(1) val url: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val scanlator: String? = null,
    @ProtoNumber(4) val read: Boolean = false,
    @ProtoNumber(5) val bookmark: Boolean = false,
    /** The page index reading stopped at. */
    @ProtoNumber(6) val lastPageRead: Long = 0,
    @ProtoNumber(7) val dateFetch: Long = 0,
    @ProtoNumber(8) val dateUpload: Long = 0,
    @ProtoNumber(9) val chapterNumber: Float = 0F,
    @ProtoNumber(10) val sourceOrder: Long = 0,
    @ProtoNumber(13) val memo: ByteArray = JsonObjectEmptyBytes,
    /** What Suwayomi's clients kept for the chapter. */
    @ProtoNumber(9000) val suwayomiMeta: Map<String, String> = emptyMap(),
)

/** A tracker link, which Katari has no trackers to restore; only that there is one is read. */
@Serializable
internal class MihonTracking(
    @ProtoNumber(1) val syncId: Int = 0,
)

/**
 * An extension repo or store; both messages sit at field 106. Mihon 0.19 and earlier write a repo, `{1 baseUrl, 2 name,
 * 3 shortName, 4 website, 5 signingKeyFingerprint}`; Mihon 0.20 and later and TachiyomiSY a store, `{1 indexUrl,
 * 2 name, 3 badgeLabel, 4 contactWebsite, 5 signingKey, 6 contactDiscord, 7 isLegacy, 8 extensionListUrl}`.
 */
@Serializable
internal class MihonExtensionSource(
    @ProtoNumber(1) val url: String = "",
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val label: String? = null,
    @ProtoNumber(4) val website: String = "",
    @ProtoNumber(5) val signingKey: String? = null,
    @ProtoNumber(6) val contactDiscord: String? = null,
    @ProtoNumber(7) val isLegacy: Boolean? = null,
    @ProtoNumber(8) val extensionListUrl: String? = null,
)
