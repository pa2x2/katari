package eu.kanade.tachiyomi.data.backup.models.compatibility

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupEntryFeatureState
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.backup.stream.ReadBackup
import eu.kanade.tachiyomi.data.backup.stream.encode
import eu.kanade.tachiyomi.data.backup.stream.readBackup
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.entry.interactions.persistence.backup.EntryBackupStateCodec
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.tracking.backup.ENTRY_TRACKING_BACKUP_STATE_ID
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupRecord
import mihon.entry.interactions.tracking.backup.EntryTrackingBackupState
import org.junit.jupiter.api.Test

class KitsuScoreScaleCompatibilityTest {

    @Test
    fun `backups from before the native Kitsu scale get rated Kitsu scores doubled, current ones keep them`() {
        val entry = BackupEntry(
            source = 1,
            url = "/manga",
            tracking = listOf(
                BackupTracking(syncId = 3, libraryId = 1, score = 7.5f),
                BackupTracking(syncId = 2, libraryId = 1, score = 7.5f),
            ),
            featureStates = listOf(
                trackingState(trackingRecord(serviceId = 3, score = 7.5), trackingRecord(serviceId = 2, score = 7.5)),
            ),
        )
        val unrated = BackupEntry(
            source = 1,
            url = "/unrated",
            tracking = listOf(BackupTracking(syncId = 3, libraryId = 1, score = -1f)),
        )
        val katari = readBackup(encode(Backup(backupEntries = listOf(entry, unrated))))

        katari.summary.hasHalvedKitsuScores() shouldBe true
        val (doubled, stillUnrated) = katari.decodedEntries.onEach(BackupEntry::doubleKitsuScores)
        doubled.tracking.map { it.score }.shouldContainExactly(15f, 7.5f)
        EntryBackupStateCodec.decode(EntryTrackingBackupState.serializer(), doubled.featureStates.single().payload)
            .records.map { it.score }.shouldContainExactly(15.0, 7.5)
        stillUnrated.tracking.single().score shouldBe -1f

        readBackup(encode(Backup(kitsuScoresNative = true))).summary.hasHalvedKitsuScores() shouldBe false
        mihonBackup(nextUpdate = 0).summary.hasHalvedKitsuScores() shouldBe true
        mihonBackup(nextUpdate = 1_790_000_000_000).summary.hasHalvedKitsuScores() shouldBe false
    }

    private fun mihonBackup(nextUpdate: Long): ReadBackup {
        val bytes = ProtoBuf.encodeToByteArray(
            MihonBackup.serializer(),
            MihonBackup(listOf(MihonBackupManga(source = 1, url = "/manga", nextUpdate = nextUpdate))),
        )
        return readBackup(bytes)
    }

    private fun trackingState(vararg records: EntryTrackingBackupRecord) = BackupEntryFeatureState(
        participantId = ENTRY_TRACKING_BACKUP_STATE_ID,
        schemaVersion = ENTRY_TRACKING_BACKUP_SCHEMA_VERSION,
        payload = EntryBackupStateCodec.encode(
            EntryTrackingBackupState.serializer(),
            EntryTrackingBackupState(records.toList()),
        ),
    )

    private fun trackingRecord(serviceId: Long, score: Double) = EntryTrackingBackupRecord(
        serviceId = serviceId,
        remoteId = 1,
        libraryId = 1,
        title = "Title",
        progress = 0.0,
        total = 0,
        score = score,
        status = 1,
        startDate = 0,
        finishDate = 0,
        remoteUrl = "",
        private = false,
    )

    /** Field numbers of Mihon v0.21.0's Backup and BackupManga. */
    @Serializable
    private data class MihonBackup(
        @ProtoNumber(1) val backupManga: List<MihonBackupManga>,
    )

    @Serializable
    private data class MihonBackupManga(
        @ProtoNumber(1) val source: Long,
        @ProtoNumber(2) val url: String,
        @ProtoNumber(113) val nextUpdate: Long = 0,
    )
}
