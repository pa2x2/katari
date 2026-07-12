package tachiyomi.domain.library.model

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.Entry

/**
 * Unified library item. A single data class wraps an [Entry] plus progress and
 * sort metadata.
 */
data class LibraryItem(
    val entry: Entry,
    val categories: List<Long>,
    val sourceName: String,
    val sourceLanguage: String,
    val sourceItemOrientation: EntryItemOrientation,
    val displaySourceId: Long,
    val sourceIds: Set<Long>,
    val isLocal: Boolean,
    val isMerged: Boolean,
    val memberEntryIds: List<LibraryItemKey>,
    val memberEntries: List<Entry>,
    val progress: ProgressState,
    val latestUpload: Long,
    val lastRead: Long,
    val continueEntryId: Long?,
    val downloadCount: Int,
) {
    val key: LibraryItemKey
        get() = LibraryItemKey(entry.type, entry.id)

    val sourceId: Long
        get() = entry.source

    val title: String
        get() = entry.displayTitle

    val cover: String?
        get() = entry.thumbnailUrl

    val favorite: Boolean
        get() = entry.favorite

    val dateAdded: Long
        get() = entry.dateAdded

    val favoriteModifiedAt: Long
        get() = entry.favoriteModifiedAt ?: entry.dateAdded

    val totalCount: Long
        get() = progress.totalCount

    val consumedCount: Long
        get() = progress.consumedCount

    val unconsumedCount: Long
        get() = totalCount - consumedCount

    val hasStarted: Boolean
        get() = progress.hasStarted

    val hasBookmarks: Boolean
        get() = progress.bookmarkCount > 0

    val hasInProgress: Boolean
        get() = progress.inProgressItemId != null

    val progressFraction: Float?
        get() = progress.inProgressFraction

    val canContinue: Boolean
        get() = progress.canContinue(continueEntryId, unconsumedCount)

    companion object {
        const val MULTI_SOURCE_ID = Long.MIN_VALUE
    }
}

open class ProgressState(
    open val totalCount: Long,
    open val consumedCount: Long,
    open val hasStarted: Boolean,
    open val bookmarkCount: Long = 0L,
    open val inProgressItemId: Long? = null,
    open val inProgressFraction: Float? = null,
    private val continueMode: ContinueMode = ContinueMode.HAS_UNCONSUMED,
) {
    fun canContinue(continueEntryId: Long?, unconsumedCount: Long): Boolean {
        return when (continueMode) {
            ContinueMode.HAS_UNCONSUMED -> unconsumedCount > 0
            ContinueMode.TARGET_AVAILABLE -> continueEntryId != null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressState) return false
        if (this::class != other::class) return false

        return totalCount == other.totalCount &&
            consumedCount == other.consumedCount &&
            hasStarted == other.hasStarted &&
            bookmarkCount == other.bookmarkCount &&
            inProgressItemId == other.inProgressItemId &&
            inProgressFraction == other.inProgressFraction &&
            continueMode == other.continueMode
    }

    override fun hashCode(): Int {
        var result = totalCount.hashCode()
        result = 31 * result + consumedCount.hashCode()
        result = 31 * result + hasStarted.hashCode()
        result = 31 * result + bookmarkCount.hashCode()
        result = 31 * result + (inProgressItemId?.hashCode() ?: 0)
        result = 31 * result + (inProgressFraction?.hashCode() ?: 0)
        result = 31 * result + continueMode.hashCode()
        return result
    }

    override fun toString(): String {
        return "ProgressState(" +
            "totalCount=$totalCount, " +
            "consumedCount=$consumedCount, " +
            "hasStarted=$hasStarted, " +
            "bookmarkCount=$bookmarkCount, " +
            "inProgressItemId=$inProgressItemId, " +
            "inProgressFraction=$inProgressFraction, " +
            "continueMode=$continueMode" +
            ")"
    }

    enum class ContinueMode {
        HAS_UNCONSUMED,
        TARGET_AVAILABLE,
    }

    @Deprecated(
        message = "Use entry-neutral ProgressState with consumedCount and bookmarkCount.",
        replaceWith = ReplaceWith(
            "ProgressState(totalCount = totalCount, consumedCount = readCount, " +
                "hasStarted = hasStarted, bookmarkCount = bookmarkCount)",
        ),
    )
    data class ChapterProgress(
        override val totalCount: Long,
        val readCount: Long,
        override val bookmarkCount: Long,
        override val hasStarted: Boolean,
    ) : ProgressState(
        totalCount = totalCount,
        consumedCount = readCount,
        hasStarted = hasStarted,
        bookmarkCount = bookmarkCount,
        continueMode = ContinueMode.HAS_UNCONSUMED,
    ) {
        override val consumedCount: Long
            get() = readCount
    }

    @Deprecated(
        message = "Use entry-neutral ProgressState with consumedCount and inProgressItemId.",
        replaceWith = ReplaceWith(
            "ProgressState(totalCount = totalCount, consumedCount = completedCount, " +
                "hasStarted = hasStarted, inProgressItemId = inProgressChapterId, " +
                "inProgressFraction = inProgressFraction, continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE)",
        ),
    )
    data class EpisodeProgress(
        override val totalCount: Long,
        val completedCount: Long,
        val watchedCount: Long,
        val inProgressChapterId: Long?,
        override val inProgressFraction: Float?,
        override val hasStarted: Boolean,
    ) : ProgressState(
        totalCount = totalCount,
        consumedCount = completedCount,
        hasStarted = hasStarted,
        inProgressItemId = inProgressChapterId,
        inProgressFraction = inProgressFraction,
        continueMode = ContinueMode.TARGET_AVAILABLE,
    ) {
        override val consumedCount: Long
            get() = completedCount
    }
}
