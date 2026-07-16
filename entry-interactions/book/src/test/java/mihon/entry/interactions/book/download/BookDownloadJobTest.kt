package mihon.entry.interactions.book.download

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookDownloadJobTest {
    @Test
    fun `wifi preference blocks cellular but allows validated wifi`() {
        assertFalse(isBookDownloadNetworkAllowed(isOnline = true, isWifi = false, requireWifi = true))
        assertTrue(isBookDownloadNetworkAllowed(isOnline = true, isWifi = true, requireWifi = true))
    }

    @Test
    fun `any validated network is allowed when wifi is not required`() {
        assertTrue(isBookDownloadNetworkAllowed(isOnline = true, isWifi = false, requireWifi = false))
        assertFalse(isBookDownloadNetworkAllowed(isOnline = false, isWifi = true, requireWifi = false))
    }
}
