package mihon.entry.interactions.book.download

import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
internal class BookDownloadResourceBudgetTest : BookDownloaderFixture() {
    @Test
    fun `download rejects a dependency set above the resource count budget`() = runTest {
        val result = downloadWithBudget(
            BookDownloadResourceBudget(maxResourceCount = 1, maxEncodedBytes = 1_024),
        )

        assertEquals(BookDownloadFailure.Reason.INTEGRITY, result.failure?.reason)
        assertNull(result.completedPackage)
    }

    @Test
    fun `download counts actual bytes for resources without declared sizes`() = runTest {
        val primaryBytes = "<p>Budgeted</p>".encodeToByteArray()
        val assetBytes = byteArrayOf(1, 2, 3, 4)
        val result = downloadWithBudget(
            BookDownloadResourceBudget(
                maxResourceCount = 2,
                maxEncodedBytes = primaryBytes.size + assetBytes.size - 1L,
            ),
            primaryBytes = primaryBytes,
            assetBytes = assetBytes,
        )

        assertEquals(BookDownloadFailure.Reason.INTEGRITY, result.failure?.reason)
        assertNull(result.completedPackage)
    }
}
