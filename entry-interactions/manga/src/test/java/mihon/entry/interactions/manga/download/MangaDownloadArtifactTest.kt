package mihon.entry.interactions.manga.download

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class MangaDownloadArtifactTest {

    @Test
    fun `completed page directory is a discoverable artifact`() {
        val directory = directory(file("001.jpg", size = 42L))

        directory.isValidMangaChapterArtifact() shouldBe true
    }

    @Test
    fun `metadata-only directory is not a discoverable artifact`() {
        val directory = directory(file("ComicInfo.xml", size = 42L))

        directory.isValidMangaChapterArtifact() shouldBe false
    }

    @Test
    fun `empty archive is not a discoverable artifact`() {
        val archive = file("Chapter 1.cbz", size = 0L)

        archive.isValidMangaChapterArtifact() shouldBe false
    }

    private fun directory(vararg children: UniFile): UniFile = mockk {
        every { isFile } returns false
        every { isDirectory } returns true
        every { listFiles() } returns children
    }

    private fun file(name: String, size: Long): UniFile = mockk {
        every { isFile } returns true
        every { isDirectory } returns false
        every { this@mockk.name } returns name
        every { length() } returns size
    }
}
