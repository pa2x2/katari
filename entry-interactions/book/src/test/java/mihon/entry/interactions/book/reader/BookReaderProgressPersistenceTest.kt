package mihon.entry.interactions.book.reader

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookLocator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookReaderProgressPersistenceTest : BookReaderSessionFixture() {
    @Test
    fun `saved locator is reconciled against the prepared model and discarded when that fails`() = runTest {
        val sourceLocator = BookLocator("old-chapter.xhtml", progression = 0.4, totalProgression = 0.3)
        val targetLocator = BookLocator("new-chapter.xhtml", progression = 0.45, totalProgression = 0.3)
        val progress = bookProgress(sourceLocator, completed = false)

        val reconciled = openWithProgress(chapter(), progress, MigratingPublicationSession(targetLocator))
        assertEquals(targetLocator, reconciled.initialLocator)
        reconciled.close()

        val failing = openWithProgress(
            chapter(),
            progress,
            LocatorRestorationPublicationSession("chapter.xhtml") { error("reconciliation unavailable") },
        )
        assertNull(failing.initialLocator)
        failing.close()

        val stale = openWithProgress(
            chapter(),
            progress,
            LocatorRestorationPublicationSession("chapter.xhtml") {
                BookLocator("still-stale.xhtml", progression = 0.4)
            },
        )
        assertNull(stale.initialLocator)
        stale.close()
    }
}
