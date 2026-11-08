package eu.kanade.tachiyomi.data.backup.stream

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupAnime
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupManga
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * What a backup holds besides its entries, and what checking and restoring it needs to know about them, read without
 * decoding any entry whole.
 *
 * @property metadata the backup without entries, its profiles included without theirs.
 * @property profileEntryCounts the number of entries of each of [Backup.backupProfiles], in that order.
 * @property entrySources the source key of every entry.
 * @property entrySourceKeys every source key an entry refers to: its source, and the source of the entry it is merged
 * into.
 * @property librarySourceCounts how many library entries come from each source, by source key.
 * @property newestEntryTime the newest time any entry records: when it was added to the library or last favourited,
 * last read, or last updated, in epoch milliseconds; 0 when none records one.
 */
internal class BackupSummary(
    val metadata: Backup,
    val topLevelEntryCount: Int,
    val profileEntryCounts: List<Int>,
    val entrySources: Set<Long>,
    val entrySourceKeys: Set<Long>,
    val librarySourceCounts: Map<Long, Int>,
    val newestEntryTime: Long,
) {
    /**
     * When the backup was made, which restoring it on a synced device stamps its data with (D046). A backup made
     * before [Backup.createdAt] was written was made no earlier than the newest time its entries record, so that time
     * stands in: an edit made after it elsewhere still wins, while nothing the backup holds counts as older than it is.
     */
    val createdAt: Long
        get() = metadata.createdAt.takeIf { it > 0 } ?: newestEntryTime
}

internal class BackupSummaryBuilder {
    private var topLevelEntryCount = 0
    private val profileEntryCounts = mutableMapOf<Int, Int>()
    private val entrySources = hashSetOf<Long>()
    private val entrySourceKeys = hashSetOf<Long>()
    private val topLevelLibrarySourceCounts = hashMapOf<Long, Int>()
    private val profileLibrarySourceCounts = hashMapOf<Long, Int>()
    private var newestEntryTime = 0L

    fun add(entry: EncodedBackupEntry) {
        val profileIndex = entry.profileIndex
        if (profileIndex == null) {
            topLevelEntryCount++
        } else {
            profileEntryCounts[profileIndex] = (profileEntryCounts[profileIndex] ?: 0) + 1
        }

        val summary = when (entry.format) {
            BackupEntryFormat.ENTRY -> entry.decode(EntrySummary.serializer()).let {
                noteTimes(it.dateAdded, it.favoriteModifiedAt, it.lastUpdate, *it.history.lastRead())
                SourceSummary(it.source, it.favorite, it.mergeTargetSource)
            }
            BackupEntryFormat.LEGACY_MANGA -> entry.decode(LegacyMangaSummary.serializer()).let {
                noteTimes(it.dateAdded, it.favoriteModifiedAt, *it.history.lastRead())
                SourceSummary(it.source, it.favorite, it.mergeTargetSource)
            }
            BackupEntryFormat.LEGACY_ANIME -> entry.decode(LegacyAnimeSummary.serializer()).let {
                noteTimes(it.dateAdded, it.favoriteModifiedAt, it.lastUpdate, *it.history.lastRead())
                SourceSummary(it.source, it.favorite, it.mergeTargetSource)
            }
        }
        val (source, favorite, mergeTargetSource) = summary
        entrySources += source
        entrySourceKeys += source
        if (favorite) {
            val counts = if (profileIndex == null) topLevelLibrarySourceCounts else profileLibrarySourceCounts
            counts[source] = (counts[source] ?: 0) + 1
        }
        mergeTargetSource?.let(entrySourceKeys::add)
    }

    private fun noteTimes(vararg times: Long?) {
        times.forEach { if (it != null && it > newestEntryTime) newestEntryTime = it }
    }

    private fun List<HistorySummary>.lastRead(): Array<Long?> = map { it.lastRead }.toTypedArray()

    fun build(metadata: Backup) = BackupSummary(
        metadata = metadata,
        topLevelEntryCount = topLevelEntryCount,
        profileEntryCounts = metadata.backupProfiles.indices.map { profileEntryCounts[it] ?: 0 },
        entrySources = entrySources,
        entrySourceKeys = entrySourceKeys,
        // Restore reads only the profiles when there are any, and older backups repeat a profile's entries at the top
        librarySourceCounts = if (metadata.backupProfiles.isEmpty()) {
            topLevelLibrarySourceCounts
        } else {
            profileLibrarySourceCounts
        },
        newestEntryTime = newestEntryTime,
    )
}

/* The fields of each entry format the summary reads, with the numbers of the full classes. */

private data class SourceSummary(
    val source: Long,
    val favorite: Boolean,
    val mergeTargetSource: Long?,
)

/** [eu.kanade.tachiyomi.data.backup.models.BackupHistory] */
@Serializable
private class HistorySummary(@ProtoNumber(2) val lastRead: Long = 0)

/** [BackupEntry] */
@Serializable
private class EntrySummary(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(10) val dateAdded: Long = 0,
    @ProtoNumber(14) val favorite: Boolean = true,
    @ProtoNumber(17) val history: List<HistorySummary> = emptyList(),
    @ProtoNumber(20) val favoriteModifiedAt: Long? = null,
    @ProtoNumber(26) val mergeTargetSource: Long? = null,
    @ProtoNumber(33) val lastUpdate: Long = 0,
)

/** [LegacyBackupManga], whose field 113 is the merge target's source only when field 114 gives its URL. */
@Serializable
private class LegacyMangaSummary(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(13) val dateAdded: Long = 0,
    @ProtoNumber(100) val favorite: Boolean = true,
    @ProtoNumber(104) val history: List<HistorySummary> = emptyList(),
    @ProtoNumber(107) val favoriteModifiedAt: Long? = null,
    @ProtoNumber(113) val mergeTargetSourceOrNextUpdate: Long? = null,
    @ProtoNumber(114) val mergeTargetUrl: String? = null,
) {
    val mergeTargetSource: Long?
        get() = mergeTargetSourceOrNextUpdate?.takeIf { mergeTargetUrl != null }
}

/** [LegacyBackupAnime] */
@Serializable
private class LegacyAnimeSummary(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(7) val dateAdded: Long = 0,
    @ProtoNumber(10) val history: List<HistorySummary> = emptyList(),
    @ProtoNumber(12) val favorite: Boolean = true,
    @ProtoNumber(14) val lastUpdate: Long = 0,
    @ProtoNumber(17) val favoriteModifiedAt: Long? = null,
    @ProtoNumber(33) val mergeTargetSource: Long? = null,
)
