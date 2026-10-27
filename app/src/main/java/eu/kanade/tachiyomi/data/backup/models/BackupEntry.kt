package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.core.common.extensions.JsonObjectEmptyBytes
import mihon.entry.interactions.state.EntryProgressStateSnapshot
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryStatus

@Serializable
class BackupEntry(
    @ProtoNumber(1) var source: Long,
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var artist: String? = null,
    @ProtoNumber(5) var author: String? = null,
    @ProtoNumber(6) var description: String? = null,
    @ProtoNumber(7) var genre: List<String> = emptyList(),
    @ProtoNumber(8) var status: Int = 0,
    @ProtoNumber(9) var thumbnailUrl: String? = null,
    @ProtoNumber(10) var dateAdded: Long = 0,
    @ProtoNumber(11) var chapters: List<BackupChapter> = emptyList(),
    @ProtoNumber(12) var categories: List<Long> = emptyList(),
    @ProtoNumber(13) var tracking: List<BackupTracking> = emptyList(),
    @ProtoNumber(14) var favorite: Boolean = true,
    @ProtoNumber(15) var chapterFlags: Long = 0,
    @ProtoNumber(16) var viewerFlags: Long = 0,
    @ProtoNumber(17) var history: List<BackupHistory> = emptyList(),
    @ProtoNumber(18) var updateStrategy: EntryUpdateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(19) var lastModifiedAt: Long = 0,
    @ProtoNumber(20) var favoriteModifiedAt: Long? = null,
    @ProtoNumber(21) var excludedScanlators: List<String> = emptyList(),
    @ProtoNumber(22) var version: Long = 0,
    @ProtoNumber(23) var notes: String = "",
    @ProtoNumber(24) var initialized: Boolean = false,
    @ProtoNumber(25) var displayName: String? = null,
    @ProtoNumber(26) var mergeTargetSource: Long? = null,
    @ProtoNumber(27) var mergeTargetUrl: String? = null,
    @ProtoNumber(28) var mergePosition: Int? = null,
    @ProtoNumber(29) var memo: ByteArray = JsonObjectEmptyBytes,
    @ProtoNumber(30) var playbackStates: List<BackupPlaybackState> = emptyList(),
    @ProtoNumber(31) var playbackPreferences: BackupPlaybackPreferences? = null,
    @ProtoNumber(32) var coverLastModified: Long = 0,
    @ProtoNumber(33) var lastUpdate: Long = 0,
    @ProtoNumber(34) var mergeTargetType: EntryType? = null,
    @ProtoNumber(35) var downloadPreferences: BackupDownloadPreferences? = null,
    @ProtoNumber(36) var progressStates: List<BackupEntryProgressState> = emptyList(),
    @ProtoNumber(37) var viewerSettingOverrides: List<BackupViewerSettingOverride> = emptyList(),
    @ProtoNumber(38) var featureStates: List<BackupEntryFeatureState> = emptyList(),
    @ProtoNumber(39) var libraryPinned: Boolean = false,
    @ProtoNumber(40) var activitySessions: List<BackupActivitySession> = emptyList(),
    @ProtoNumber(41) var activityCompletions: List<BackupActivityCompletion> = emptyList(),
    @ProtoNumber(42) var statisticsEpoch: Long? = null,
    /** Lowercase [tachiyomi.domain.library.update.model.EntryUpdateMode] name; null follows the library rules. */
    @ProtoNumber(43) var updateMode: String? = null,
    /** Whether this entry, as a member of a merged entry, shows its chapters in the merged chapter list. */
    @ProtoNumber(44) var mergeMemberExpanded: Boolean = false,
    /** Epoch millis of the next expected source update; 0 when the backup carries none. */
    @ProtoNumber(45) var nextUpdate: Long = 0,
    @ProtoNumber(46) var mergedBySource: Boolean = false,
    @ProtoNumber(47) var downloadTitle: String? = null,
    @ProtoNumber(48) var backgroundUrl: String? = null,
    /** The groups the chapters belong to, in reading order; backed up with the chapters. */
    @ProtoNumber(49) var chapterGroups: List<BackupChapterGroup> = emptyList(),
    /** Keys of the chapter groups the chapter list shows open; the empty key is the chapters in no group. */
    @ProtoNumber(50) var expandedChapterGroups: List<String> = emptyList(),
    @ProtoNumber(100) var type: EntryType = EntryType.MANGA,
) {
    fun toEntry(): Entry {
        return Entry.create().copy(
            url = url,
            title = title,
            displayName = displayName,
            artist = artist,
            author = author,
            description = description,
            genre = genre,
            status = EntryStatus.from(status),
            thumbnailUrl = thumbnailUrl,
            favorite = favorite,
            libraryPinned = libraryPinned,
            source = source,
            dateAdded = dateAdded,
            viewerFlags = viewerFlags,
            chapterFlags = chapterFlags,
            updateStrategy = updateStrategy,
            lastModifiedAt = lastModifiedAt,
            favoriteModifiedAt = favoriteModifiedAt,
            version = version,
            notes = notes,
            initialized = initialized,
            memo = MemoColumnAdapter.decode(memo),
            coverLastModified = coverLastModified,
            lastUpdate = lastUpdate,
            nextUpdate = nextUpdate,
            type = type,
            mergedBySource = mergedBySource,
            downloadTitle = downloadTitle,
            backgroundUrl = backgroundUrl,
        )
    }
}

@Serializable
data class BackupEntryFeatureState(
    @ProtoNumber(1) var participantId: String,
    @ProtoNumber(2) var schemaVersion: Int,
    @ProtoNumber(3) var payload: ByteArray,
)

@Serializable
class BackupChapter(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var scanlator: String? = null,
    @ProtoNumber(4) var read: Boolean = false,
    @ProtoNumber(5) var bookmark: Boolean = false,
    @ProtoNumber(6) var lastPageRead: Long = 0,
    @ProtoNumber(7) var dateFetch: Long = 0,
    @ProtoNumber(8) var dateUpload: Long = 0,
    @ProtoNumber(9) var chapterNumber: Double = 0.0,
    @ProtoNumber(10) var sourceOrder: Long = 0,
    @ProtoNumber(11) var lastModifiedAt: Long = 0,
    @ProtoNumber(12) var version: Long = 0,
    @ProtoNumber(13) var memo: ByteArray = JsonObjectEmptyBytes,
    @ProtoNumber(14) var summary: String? = null,
    @ProtoNumber(15) var previewUrl: String? = null,
    /** [ChapterKind.code]. */
    @ProtoNumber(16) var kind: Int = ChapterKind.REGULAR.code,
    /** Key of one of the entry's [BackupEntry.chapterGroups]. */
    @ProtoNumber(17) var groupKey: String? = null,
    @ProtoNumber(18) var dateText: String? = null,
) {
    fun toEntryChapter(entryId: Long): tachiyomi.domain.entry.model.EntryChapter {
        return tachiyomi.domain.entry.model.EntryChapter.create().copy(
            entryId = entryId,
            url = url,
            name = name,
            chapterNumber = chapterNumber,
            scanlator = scanlator,
            read = read,
            bookmark = bookmark,
            dateFetch = dateFetch,
            dateUpload = dateUpload,
            sourceOrder = sourceOrder,
            lastModifiedAt = lastModifiedAt,
            version = version,
            memo = MemoColumnAdapter.decode(memo),
            summary = summary,
            previewUrl = previewUrl,
            kind = ChapterKind.fromCode(kind),
            groupKey = groupKey,
            dateText = dateText,
        )
    }
}

@Serializable
class BackupChapterGroup(
    @ProtoNumber(1) var key: String,
    @ProtoNumber(2) var title: String,
)

@Serializable
data class BackupHistory(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var lastRead: Long,
    @ProtoNumber(3) var readDuration: Long = 0,
)

@Serializable
data class BackupActivitySession(
    @ProtoNumber(1) var sessionId: String,
    @ProtoNumber(2) var startedAt: Long,
    @ProtoNumber(3) var endedAt: Long,
    @ProtoNumber(4) var duration: Long,
    @ProtoNumber(5) var lastSequence: Long,
    @ProtoNumber(6) var segments: List<BackupActivitySegment> = emptyList(),
)

@Serializable
data class BackupActivitySegment(
    @ProtoNumber(1) var chapterUrl: String? = null,
    @ProtoNumber(2) var localDate: String,
    @ProtoNumber(3) var timeZoneId: String,
    @ProtoNumber(4) var startedAt: Long,
    @ProtoNumber(5) var endedAt: Long,
    @ProtoNumber(6) var duration: Long,
)

@Serializable
data class BackupActivityCompletion(
    @ProtoNumber(1) var eventId: String,
    @ProtoNumber(2) var chapterUrl: String? = null,
    @ProtoNumber(3) var sessionId: String? = null,
    @ProtoNumber(4) var occurredAt: Long,
    @ProtoNumber(5) var localDate: String,
    @ProtoNumber(6) var timeZoneId: String,
    @ProtoNumber(7) var cause: String,
)

@Serializable
data class BackupPlaybackState(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var positionMs: Long,
    @ProtoNumber(3) var durationMs: Long,
    @ProtoNumber(4) var completed: Boolean,
    @ProtoNumber(5) var lastWatchedAt: Long,
)

@Serializable
data class BackupEntryProgressState(
    @ProtoNumber(1) var contentKey: String = "",
    @ProtoNumber(2) var resourceKey: String,
    @ProtoNumber(3) var sourceChildKey: String? = null,
    @ProtoNumber(4) var resourceRevision: String? = null,
    @ProtoNumber(5) var locatorKind: String,
    @ProtoNumber(6) var position: Long? = null,
    @ProtoNumber(7) var extent: Long? = null,
    @ProtoNumber(8) var progression: Double? = null,
    @ProtoNumber(9) var totalProgression: Double? = null,
    @ProtoNumber(10) var extensions: ByteArray = JsonObjectEmptyBytes,
    @ProtoNumber(11) var completed: Boolean = false,
    @ProtoNumber(12) var locatorUpdatedAt: Long = 0,
    @ProtoNumber(13) var completionUpdatedAt: Long = 0,
)

@Serializable
data class BackupViewerSettingOverride(
    @ProtoNumber(1) var providerId: String,
    @ProtoNumber(2) var settingKey: String,
    @ProtoNumber(3) var encodedValue: String,
    @ProtoNumber(4) var updatedAt: Long = 0,
)

@Serializable
data class BackupPlaybackPreferences(
    @ProtoNumber(1) var dubKey: String? = null,
    @ProtoNumber(2) var streamKey: String? = null,
    @ProtoNumber(3) var sourceQualityKey: String? = null,
    @ProtoNumber(4) var playerQualityMode: String = "auto",
    @ProtoNumber(5) var playerQualityHeight: Int? = null,
    @ProtoNumber(6) var updatedAt: Long = 0,
    @ProtoNumber(7) var subtitleOffsetX: Double? = null,
    @ProtoNumber(8) var subtitleOffsetY: Double? = null,
    @ProtoNumber(9) var subtitleTextSize: Double? = null,
    @ProtoNumber(10) var subtitleTextColor: Int? = null,
    @ProtoNumber(11) var subtitleBackgroundColor: Int? = null,
    @ProtoNumber(12) var subtitleBackgroundOpacity: Double? = null,
    @ProtoNumber(13) var subtitleKey: String? = null,
    /** "built_in" or "mpv"; null while the player chooses. */
    @ProtoNumber(14) var playerEngine: String? = null,
    @ProtoNumber(15) var serverKey: String? = null,
    /** "none", "outline" or "shadow"; null keeps the outline. */
    @ProtoNumber(16) var subtitleTextEdge: String? = null,
)

@Serializable
data class BackupDownloadPreferences(
    @ProtoNumber(1) var dubKey: String? = null,
    @ProtoNumber(2) var streamKey: String? = null,
    @ProtoNumber(3) var subtitleKey: String? = null,
    @ProtoNumber(4) var qualityMode: String = "balanced",
    @ProtoNumber(5) var updatedAt: Long = 0,
    @ProtoNumber(6) var serverKey: String? = null,
    @ProtoNumber(7) var audioKey: String? = null,
)

fun Entry.toBackupEntry(): BackupEntry {
    return BackupEntry(
        source = source,
        url = url,
        title = title,
        displayName = displayName,
        artist = artist,
        author = author,
        description = description,
        genre = genre.orEmpty(),
        status = status.value,
        thumbnailUrl = thumbnailUrl,
        favorite = favorite,
        libraryPinned = libraryPinned,
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        chapterFlags = chapterFlags,
        updateStrategy = updateStrategy,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
        version = version,
        notes = notes,
        initialized = initialized,
        memo = MemoColumnAdapter.encode(memo),
        coverLastModified = coverLastModified,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate,
        type = type,
        mergedBySource = mergedBySource,
        downloadTitle = downloadTitle,
        backgroundUrl = backgroundUrl,
    )
}

fun tachiyomi.domain.entry.model.EntryChapter.toBackupChapter(): BackupChapter {
    return BackupChapter(
        url = url,
        name = name,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = MemoColumnAdapter.encode(memo),
        summary = summary,
        previewUrl = previewUrl,
        kind = kind.code,
        groupKey = groupKey,
        dateText = dateText,
    )
}

internal fun EntryProgressStateSnapshot.toBackupEntryProgressState(): BackupEntryProgressState {
    return BackupEntryProgressState(
        contentKey = contentKey,
        resourceKey = resourceKey,
        sourceChildKey = sourceChildKey,
        resourceRevision = resourceRevision,
        locatorKind = locator.kind,
        position = locator.position,
        extent = locator.extent,
        progression = locator.progression,
        totalProgression = locator.totalProgression,
        extensions = MemoColumnAdapter.encode(locator.extensions),
        completed = completed,
        locatorUpdatedAt = locatorUpdatedAt,
        completionUpdatedAt = completionUpdatedAt,
    )
}

internal fun BackupEntryProgressState.toEntryProgressStateSnapshot(): EntryProgressStateSnapshot {
    return EntryProgressStateSnapshot(
        contentKey = contentKey,
        resourceKey = resourceKey,
        sourceChildKey = sourceChildKey,
        resourceRevision = resourceRevision,
        locator = EntryProgressLocator(
            kind = locatorKind,
            position = position,
            extent = extent,
            progression = progression,
            totalProgression = totalProgression,
            extensions = MemoColumnAdapter.decode(extensions),
        ),
        completed = completed,
        locatorUpdatedAt = locatorUpdatedAt,
        completionUpdatedAt = completionUpdatedAt,
    )
}
