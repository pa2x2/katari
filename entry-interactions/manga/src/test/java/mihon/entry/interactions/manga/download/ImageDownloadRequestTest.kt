package mihon.entry.interactions.manga.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.ResumableEntryImageSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun `resumable source receives existing size and appends a partial response`() = runTest {
        val response = response(code = 206)
        val source = mockk<ResumableEntryImageSource> {
            coEvery { getImage(page, progress, 37L) } returns response
        }

        val download = source.getImageForDownload(page, progress, partialFile)

        download.response shouldBe response
        download.appendToExistingFile shouldBe true
        coVerify(exactly = 1) { source.getImage(page, progress, 37L) }
    }

    @Test
    fun `resumable source overwrites existing data when server returns full response`() = runTest {
        val source = mockk<ResumableEntryImageSource> {
            coEvery { getImage(page, progress, 37L) } returns response(code = 200)
        }

        val download = source.getImageForDownload(page, progress, partialFile)

        download.appendToExistingFile shouldBe false
    }

    @Test
    fun `non resumable source cannot append even when it returns partial response`() = runTest {
        val response = response(code = 206)
        val source = mockk<EntryImageSource> {
            coEvery { getImage(page, progress) } returns response
        }

        val download = source.getImageForDownload(page, progress, partialFile)

        download.appendToExistingFile shouldBe false
        coVerify(exactly = 1) { source.getImage(page, progress) }
    }

    private fun response(code: Int): Response = mockk {
        every { this@mockk.code } returns code
    }
}
