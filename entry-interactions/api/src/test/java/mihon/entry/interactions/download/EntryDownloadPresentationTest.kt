package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

class EntryDownloadPresentationTest {

    @Test
    fun `queue item rejects a media phase that contradicts shared state`() {
        shouldThrow<IllegalArgumentException> {
            queueItem(
                state = EntryDownloadState.QUEUE,
                presentation = EntryDownloadPresentation(EntryDownloadPhase.TRANSFERRING),
            )
        }
    }

    @Test
    fun `queue group rejects items attributed to another media source`() {
        val item = queueItem(
            state = EntryDownloadState.QUEUE,
            presentation = EntryDownloadPresentation(EntryDownloadPhase.QUEUED),
        )

        shouldThrow<IllegalArgumentException> {
            EntryDownloadQueueGroup(
                sourceId = item.sourceId + 1,
                sourceName = "Other source",
                entryType = item.entryType,
                items = listOf(item),
            )
        }
    }

    @Test
    fun `shared descriptions preserve semantic progress without media strings`() {
        EntryDownloadPresentation(
            phase = EntryDownloadPhase.TRANSFERRING,
            progress = EntryDownloadProgress.Percent(42),
        ).description() shouldBe EntryDownloadMessage.Resource(
            resource = MR.strings.download_progress_percent,
            args = listOf(42),
        )
        EntryDownloadPresentation(
            phase = EntryDownloadPhase.TRANSFERRING,
            progress = EntryDownloadProgress.Units(completed = 3, total = 7),
        ).description() shouldBe EntryDownloadMessage.Resource(
            resource = MR.strings.download_progress_units,
            args = listOf(3, 7),
        )
    }

    @Test
    fun `failed phase uses structured failure before generic fallback`() {
        val failure = EntryDownloadMessage.Resource(MR.strings.download_notifier_insufficient_storage)

        EntryDownloadPresentation(EntryDownloadPhase.FAILED, failure = failure).description() shouldBe failure
        EntryDownloadPresentation(EntryDownloadPhase.FAILED).description() shouldBe
            EntryDownloadMessage.Resource(MR.strings.chapter_error)
    }

    @Test
    fun `queue failure rejects downloader supplied display text`() {
        shouldThrow<IllegalArgumentException> {
            EntryDownloadPresentation(
                phase = EntryDownloadPhase.FAILED,
                failure = EntryDownloadMessage.Text("raw transport exception"),
            )
        }
    }

    private fun queueItem(
        state: EntryDownloadState,
        presentation: EntryDownloadPresentation,
    ) = EntryDownloadQueueItem(
        identity = EntryDownloadIdentity(
            profileId = 1L,
            entryType = EntryType.MANGA,
            entryId = 2L,
            sourceId = 3L,
            childId = 4L,
        ),
        state = state,
        title = "Entry",
        subtitle = "Item",
        dateUpload = 0L,
        chapterNumber = 1.0,
        progress = 0,
        progressMax = 100,
        presentation = presentation,
    )
}
