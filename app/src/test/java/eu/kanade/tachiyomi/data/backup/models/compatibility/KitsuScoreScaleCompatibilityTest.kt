package eu.kanade.tachiyomi.data.backup.models.compatibility

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupEntryFeatureState
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
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
        val katari = Backup(backupEntries = listOf(entry, unrated))

        katari.hasHalvedKitsuScores() shouldBe true
        katari.allEntries().forEach(BackupEntry::doubleKitsuScores)
        entry.tracking.map { it.score }.shouldContainExactly(15f, 7.5f)
        EntryBackupStateCodec.decode(EntryTrackingBackupState.serializer(), entry.featureStates.single().payload)
            .records.map { it.score }.shouldContainExactly(15.0, 7.5)
        unrated.tracking.single().score shouldBe -1f

        roundTrip(Backup(kitsuScoresNative = true)).hasHalvedKitsuScores() shouldBe false
        mihonBackup(nextUpdate = 0).hasHalvedKitsuScores() shouldBe true
        mihonBackup(nextUpdate = 1_790_000_000_000).hasHalvedKitsuScores() shouldBe false
    }

    private fun roundTrip(backup: Backup): Backup {
        return ProtoBuf.decodeFromByteArray(
            Backup.serializer(),
            ProtoBuf.encodeToByteArray(Backup.serializer(), backup),
        )
    }

    private fun mihonBackup(nextUpdate: Long): Backup {
        val bytes = ProtoBuf.encodeToByteArray(
            MihonBackup.serializer(),
            MihonBackup(listOf(MihonBackupManga(source = 1, url = "/manga", nextUpdate = nextUpdate))),
        )
        return ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
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
