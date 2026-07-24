package mihon.entry.interactions.host.tracking

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.EntryTrackingBackupRecord
import org.junit.jupiter.api.Test

class AppEntryTrackingBackupHostTest {

    @Test
    fun `restore preserves local tracker state while merging backup identity and progress`() {
        val existing = trackingRecord(
            remoteId = 1,
            libraryId = 2,
            progress = 8.0,
            title = "Current title",
            total = 20,
            score = 9.0,
            status = 3,
            remoteUrl = "current-url",
            private = true,
        )
        val incoming = trackingRecord(
            remoteId = 11,
            libraryId = 12,
            progress = 5.0,
            title = "Backup title",
            total = 10,
            score = 4.0,
            status = 1,
            remoteUrl = "backup-url",
            private = false,
        )

        incoming.mergeForRestore(existing) shouldBe existing.copy(
            remoteId = incoming.remoteId,
            libraryId = incoming.libraryId,
        )
        incoming.copy(progress = 12.0).mergeForRestore(existing) shouldBe existing.copy(
            remoteId = incoming.remoteId,
            libraryId = incoming.libraryId,
            progress = 12.0,
        )
    }
}

private fun trackingRecord(
    remoteId: Long,
    libraryId: Long,
    progress: Double,
    title: String,
    total: Long,
    score: Double,
    status: Long,
    remoteUrl: String,
    private: Boolean,
) = EntryTrackingBackupRecord(
    serviceId = 7,
    remoteId = remoteId,
    libraryId = libraryId,
    title = title,
    progress = progress,
    total = total,
    score = score,
    status = status,
    startDate = 100,
    finishDate = 200,
    remoteUrl = remoteUrl,
    private = private,
)
