package mihon.entry.interactions.book.download

import android.app.Application
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.content.BookMaterializationCache
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.nio.file.Files
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class DownloadedBookContentSessionTest {
    @Test
    fun `session close releases outstanding downloaded streams`() = runTest {
        val cache = BookMaterializationCache(
            application = mockk<Application>(relaxed = true),
            directory = Files.createTempDirectory("katari-book-download-close").toFile(),
        )
        val session = DownloadedBookContentSession(fixture().complete(content = "chapter"), cache)
        val opened = session.openResource("chapter").getOrThrow()

        session.close()

        assertFailsWith<IOException> { opened.stream.read() }
    }
}
