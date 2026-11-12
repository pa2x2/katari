package app.katari.sync.client.kind.progress

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProgressChapterMergeTest {

    @Test
    fun `devices that link a position to different chapters both take the chapter of the newer position`() {
        val onA = position(chapterKey = "chapter-old-url", readAt = 1000, device = 1)
        val onB = position(chapterKey = "chapter-new-url", readAt = 2000, device = 2)

        ProgressKind.merge(local = onA, incoming = onB).chapterKey shouldBe "chapter-new-url"
        ProgressKind.merge(local = onB, incoming = onA).chapterKey shouldBe "chapter-new-url"
    }

    @Test
    fun `a newer position from a device that links it to no chapter keeps the chapter`() {
        val linked = position(chapterKey = "chapter", readAt = 1000, device = 1)
        val unlinked = position(chapterKey = null, readAt = 2000, device = 2)

        ProgressKind.merge(local = linked, incoming = unlinked).chapterKey shouldBe "chapter"
        ProgressKind.merge(local = unlinked, incoming = linked).chapterKey shouldBe "chapter"
    }

    private fun position(chapterKey: String?, readAt: Long, device: Long) = ProgressPayload(
        entryKey = "entry",
        contentKey = "",
        resourceKey = "/show/1",
        chapterKey = chapterKey,
        locator = Stamped(
            ProgressLocator(kind = "time", position = readAt),
            HybridTimestamp(physicalMillis = readAt, counter = 0, device = DeviceId(device)),
        ),
        completed = Stamped(false, Stamped.UNKNOWN_EDIT),
    )
}
