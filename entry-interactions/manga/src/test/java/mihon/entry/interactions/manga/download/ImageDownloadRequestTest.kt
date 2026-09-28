package mihon.entry.interactions.manga.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.ResumableEntryImageSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Response
import org.junit.jupiter.api.Test

class ImageDownloadRequestTest {

    private val page = EntryImagePage(index = 0, imageUrl = "https://example.invalid/page.jpg")
    private val progress = mockk<ProgressListener>()
    private val partialFile = mockk<UniFile> {
        every { length() } returns 37L
    }

    @Test
    fun `a resumable source appends only when the server answers with partial content`() = runTest {
        val partial = mockk<ResumableEntryImageSource> {
            coEvery { getImage(page, progress, 37L) } returns response(code = 206)
        }
        val full = mockk<ResumableEntryImageSource> {
            coEvery { getImage(page, progress, 37L) } returns response(code = 200)
        }

        partial.getImageForDownload(page, progress, partialFile).appendToExistingFile shouldBe true
        full.getImageForDownload(page, progress, partialFile).appendToExistingFile shouldBe false
    }

    @Test
    fun `a non resumable source never appends even when it returns partial content`() = runTest {
        val source = mockk<EntryImageSource> {
            coEvery { getImage(page, progress) } returns response(code = 206)
        }

        source.getImageForDownload(page, progress, partialFile).appendToExistingFile shouldBe false
    }

    private fun response(code: Int): Response = mockk {
        every { this@mockk.code } returns code
    }
}
