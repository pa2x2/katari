package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.entry.model.EntryStatus

@Serializable
class LegacyBackupManga(
    @ProtoNumber(1) var source: Long,
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var artist: String? = null,
    @ProtoNumber(5) var author: String? = null,
    @ProtoNumber(6) var description: String? = null,
    @ProtoNumber(7) var genre: List<String> = emptyList(),
    @ProtoNumber(8) var status: Int = 0,
    @ProtoNumber(9) var thumbnailUrl: String? = null,
    @ProtoNumber(13) var dateAdded: Long = 0,
    @ProtoNumber(14) var viewer: Int = 0,
    @ProtoNumber(16) var chapters: List<LegacyBackupChapter> = emptyList(),
    @ProtoNumber(17) var categories: List<Long> = emptyList(),
    @ProtoNumber(18) var tracking: List<BackupTracking> = emptyList(),
    @ProtoNumber(100) var favorite: Boolean = true,
    @ProtoNumber(101) var chapterFlags: Int = 0,
    @ProtoNumber(103) var viewer_flags: Int? = null,
    @ProtoNumber(104) var history: List<BackupHistory> = emptyList(),
    @ProtoNumber(105) var updateStrategy: EntryUpdateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(106) var lastModifiedAt: Long = 0,
    @ProtoNumber(107) var favoriteModifiedAt: Long? = null,
    @ProtoNumber(108) var excludedScanlators: List<String> = emptyList(),
    @ProtoNumber(109) var version: Long = 0,
    @ProtoNumber(110) var notes: String = "",
    @ProtoNumber(111) var initialized: Boolean = false,
    @ProtoNumber(112) var displayName: String? = null,
    @ProtoNumber(113) var mergeTargetSource: Long? = null,
    @ProtoNumber(114) var mergeTargetUrl: String? = null,
    @ProtoNumber(115) var mergePosition: Int? = null,
    @ProtoNumber(116) var memo: ByteArray = mihon.core.common.extensions.JsonObjectEmptyBytes,
)

@Serializable
data class LegacyBackupChapter(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var scanlator: String? = null,
    @ProtoNumber(4) var read: Boolean = false,
    @ProtoNumber(5) var bookmark: Boolean = false,
    @ProtoNumber(6) var lastPageRead: Long = 0,
    @ProtoNumber(7) var dateFetch: Long = 0,
    @ProtoNumber(8) var dateUpload: Long = 0,
    @ProtoNumber(9) var chapterNumber: Float = 0F,
    @ProtoNumber(10) var sourceOrder: Long = 0,
    @ProtoNumber(11) var lastModifiedAt: Long = 0,
    @ProtoNumber(12) var version: Long = 0,
    @ProtoNumber(13) var memo: ByteArray = mihon.core.common.extensions.JsonObjectEmptyBytes,
)

@Serializable
data class LegacyBackupAnime(
    @ProtoNumber(1) var source: Long,
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var description: String? = null,
    @ProtoNumber(5) var genre: List<String> = emptyList(),
    @ProtoNumber(6) var thumbnailUrl: String? = null,
    @ProtoNumber(7) var dateAdded: Long = 0,
    @ProtoNumber(8) var episodes: List<LegacyBackupAnimeEpisode> = emptyList(),
    @ProtoNumber(9) var categories: List<Long> = emptyList(),
    @ProtoNumber(10) var history: List<BackupHistory> = emptyList(),
    @ProtoNumber(11) var playbackStates: List<BackupPlaybackState> = emptyList(),
    @ProtoNumber(12) var favorite: Boolean = true,
    @ProtoNumber(13) var initialized: Boolean = false,
    @ProtoNumber(14) var lastUpdate: Long = 0,
    @ProtoNumber(16) var lastModifiedAt: Long = 0,
    @ProtoNumber(17) var favoriteModifiedAt: Long? = null,
    @ProtoNumber(18) var version: Long = 0,
    @ProtoNumber(19) var notes: String = "",
    @ProtoNumber(20) var displayName: String? = null,
    @ProtoNumber(21) var playbackPreferences: BackupPlaybackPreferences? = null,
    @ProtoNumber(30) var status: Long = 0,
    @ProtoNumber(31) var episodeFlags: Long = 0,
    @ProtoNumber(32) var coverLastModified: Long = 0,
    @ProtoNumber(33) var mergeTargetSource: Long? = null,
    @ProtoNumber(34) var mergeTargetUrl: String? = null,
    @ProtoNumber(35) var mergePosition: Int? = null,
)

@Serializable
data class LegacyBackupAnimeEpisode(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var watched: Boolean = false,
    @ProtoNumber(4) var completed: Boolean = false,
    @ProtoNumber(5) var dateFetch: Long = 0,
    @ProtoNumber(6) var dateUpload: Long = 0,
    @ProtoNumber(7) var episodeNumber: Float = 0F,
    @ProtoNumber(8) var sourceOrder: Long = 0,
    @ProtoNumber(9) var lastModifiedAt: Long = 0,
    @ProtoNumber(10) var version: Long = 0,
)

internal fun LegacyBackupManga.toBackupEntry(): BackupEntry {
    return BackupEntry(
        source = source,
        url = url,
        title = title,
        displayName = displayName,
        artist = artist,
        author = author,
        description = description,
        genre = genre,
        status = status,
        thumbnailUrl = thumbnailUrl,
        favorite = favorite,
        dateAdded = dateAdded,
        viewerFlags = (viewer_flags ?: viewer).toLong(),
        chapterFlags = chapterFlags.toLong(),
        updateStrategy = updateStrategy,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
        version = version,
        notes = notes,
        initialized = initialized,
        memo = memo,
        chapters = chapters.map { it.toBackupChapter() },
        categories = categories,
        tracking = tracking,
        history = history,
        excludedScanlators = excludedScanlators,
        mergeTargetSource = mergeTargetSource,
        mergeTargetUrl = mergeTargetUrl,
        mergeTargetType = EntryType.MANGA,
        mergePosition = mergePosition,
        type = EntryType.MANGA,
    )
}

internal fun LegacyBackupChapter.toBackupChapter(): BackupChapter {
    return BackupChapter(
        url = url,
        name = name,
        chapterNumber = chapterNumber.toDouble(),
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = memo,
    )
}

internal fun LegacyBackupAnime.toBackupEntry(): BackupEntry {
    return BackupEntry(
        source = source,
        url = url,
        title = title,
        displayName = displayName,
        description = description,
        genre = genre,
        status = status.toUnifiedAnimeStatus(),
        thumbnailUrl = thumbnailUrl,
        favorite = favorite,
        dateAdded = dateAdded,
        chapterFlags = episodeFlags,
        coverLastModified = coverLastModified,
        updateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
        version = version,
        notes = notes,
        initialized = initialized,
        lastUpdate = lastUpdate,
        chapters = episodes.map { it.toBackupChapter() },
        categories = categories,
        history = history,
        playbackStates = playbackStates,
        playbackPreferences = playbackPreferences,
        mergeTargetSource = mergeTargetSource,
        mergeTargetUrl = mergeTargetUrl,
        mergeTargetType = EntryType.ANIME,
        mergePosition = mergePosition,
        type = EntryType.ANIME,
    )
}

private fun Long.toUnifiedAnimeStatus(): Int {
    return when (this) {
        3L -> EntryStatus.CANCELLED.value
        4L -> EntryStatus.ON_HIATUS.value
        else -> toInt()
    }
}

internal fun LegacyBackupAnimeEpisode.toBackupChapter(): BackupChapter {
    return BackupChapter(
        url = url,
        name = name,
        chapterNumber = episodeNumber.toDouble(),
        read = completed,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
    )
}
