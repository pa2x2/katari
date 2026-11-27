package eu.kanade.tachiyomi.data.backup.conversion.mihon

import app.katari.core.common.extensions.JsonObjectEmptyBytes
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
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
    @ProtoNumber(2) val categories: List<BackupCategory> = emptyList(),
    @ProtoNumber(101) val sources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) val preferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) val sourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) val extensionSources: List<MihonExtensionSource> = emptyList(),
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
    /** The [BackupCategory.order] of each category the entry is in. */
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
