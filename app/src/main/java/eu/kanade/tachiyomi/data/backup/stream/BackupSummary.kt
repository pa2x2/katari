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
 * @property mihonEntryHasNextUpdate whether a manga at the top level carries its next update, which only Mihon writes.
 */
internal class BackupSummary(
    val metadata: Backup,
    val topLevelEntryCount: Int,
    val profileEntryCounts: List<Int>,
    val entrySources: Set<Long>,
    val entrySourceKeys: Set<Long>,
    val trackingServiceIds: Set<Long>,
    val mihonEntryHasNextUpdate: Boolean,
)

internal class BackupSummaryBuilder {
    private var topLevelEntryCount = 0
    private val profileEntryCounts = mutableMapOf<Int, Int>()
    private val entrySources = hashSetOf<Long>()
    private val entrySourceKeys = hashSetOf<Long>()
    private val trackingServiceIds = linkedSetOf<Long>()
    private var mihonEntryHasNextUpdate = false

    fun add(entry: EncodedBackupEntry) {
        val profileIndex = entry.profileIndex
        if (profileIndex == null) {
            topLevelEntryCount++
        } else {
            profileEntryCounts[profileIndex] = (profileEntryCounts[profileIndex] ?: 0) + 1
        }

        val (source, mergeTargetSource, tracking) = when (entry.format) {
            BackupEntryFormat.ENTRY -> entry.decode(EntrySummary.serializer())
                .let { Triple(it.source, it.mergeTargetSource, it.tracking) }
            BackupEntryFormat.LEGACY_MANGA -> entry.decode(LegacyMangaSummary.serializer()).let {
                if (profileIndex == null && it.nextUpdate != 0L) mihonEntryHasNextUpdate = true
                Triple(it.source, it.mergeTargetSource, it.tracking)
            }
            BackupEntryFormat.LEGACY_ANIME -> entry.decode(LegacyAnimeSummary.serializer())
                .let { Triple(it.source, it.mergeTargetSource, emptyList()) }
        }
        entrySources += source
        entrySourceKeys += source
        mergeTargetSource?.let(entrySourceKeys::add)
        tracking.mapTo(trackingServiceIds) { it.syncId.toLong() }
    }

    fun build(metadata: Backup) = BackupSummary(
        metadata = metadata,
        topLevelEntryCount = topLevelEntryCount,
        profileEntryCounts = metadata.backupProfiles.indices.map { profileEntryCounts[it] ?: 0 },
        entrySources = entrySources,
        entrySourceKeys = entrySourceKeys,
        trackingServiceIds = trackingServiceIds,
        mihonEntryHasNextUpdate = mihonEntryHasNextUpdate,
    )
}

/* The fields of each entry format the summary reads, with the numbers of the full classes. */

@Serializable
private class TrackingSummary(@ProtoNumber(1) val syncId: Int)

/** [BackupEntry] */
@Serializable
private class EntrySummary(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(13) val tracking: List<TrackingSummary> = emptyList(),
    @ProtoNumber(26) val mergeTargetSource: Long? = null,
)

/** [LegacyBackupManga], whose field 113 is the merge target's source only when field 114 gives its URL. */
@Serializable
private class LegacyMangaSummary(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(18) val tracking: List<TrackingSummary> = emptyList(),
    @ProtoNumber(113) val mergeTargetSourceOrNextUpdate: Long? = null,
    @ProtoNumber(114) val mergeTargetUrl: String? = null,
) {
    val mergeTargetSource: Long?
        get() = mergeTargetSourceOrNextUpdate?.takeIf { mergeTargetUrl != null }

    val nextUpdate: Long
        get() = mergeTargetSourceOrNextUpdate?.takeIf { mergeTargetUrl == null } ?: 0L
}

/** [LegacyBackupAnime] */
@Serializable
private class LegacyAnimeSummary(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(33) val mergeTargetSource: Long? = null,
)
