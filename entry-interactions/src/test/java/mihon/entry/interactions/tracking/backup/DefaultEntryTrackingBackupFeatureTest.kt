package mihon.entry.interactions

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class DefaultEntryTrackingBackupFeatureTest {

    @Test
    fun `Tracking owns profile-scoped portable state`() = runTest {
        val record = EntryTrackingBackupRecord(
            serviceId = 1,
            remoteId = 2,
            libraryId = null,
            title = "Entry",
            progress = 3.0,
            total = 4,
            score = 5.0,
            status = 6,
            startDate = 7,
            finishDate = 8,
            remoteUrl = "/track",
            private = true,
        )
        var restored: Triple<Long, Long, List<EntryTrackingBackupRecord>>? = null
        val host = object : EntryTrackingBackupHost {
            override suspend fun snapshot(profileId: Long, entryId: Long) = listOf(record)

            override suspend fun restore(
                profileId: Long,
                entryId: Long,
                records: List<EntryTrackingBackupRecord>,
            ) {
                restored = Triple(profileId, entryId, records)
            }
        }
        val feature = DefaultEntryTrackingBackupFeature(host)
        val entry = Entry.create().copy(id = 12)

        val state = feature.snapshot(9, entry)
        state shouldBe EntryTrackingBackupState(listOf(record))

        feature.restore(9, entry, state!!)
        restored shouldBe Triple(9L, 12L, listOf(record))
    }
}
