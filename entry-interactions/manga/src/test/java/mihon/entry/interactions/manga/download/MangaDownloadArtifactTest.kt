package mihon.entry.interactions.manga.download

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class MangaDownloadArtifactTest {

    @Test
    fun `only directories with pages and non-empty archives are discoverable artifacts`() {
        directory(file("001.jpg", size = 42L)).isValidMangaChapterArtifact() shouldBe true
        directory(file("ComicInfo.xml", size = 42L)).isValidMangaChapterArtifact() shouldBe false
        file("Chapter 1.cbz", size = 0L).isValidMangaChapterArtifact() shouldBe false
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
