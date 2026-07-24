package mihon.entry.interactions

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntryDownloadJobTest {
    @Test
    fun `wifi preference blocks cellular but allows validated wifi`() {
        assertFalse(isEntryDownloadNetworkAllowed(isOnline = true, isWifi = false, requireWifi = true))
        assertTrue(isEntryDownloadNetworkAllowed(isOnline = true, isWifi = true, requireWifi = true))
    }

    @Test
    fun `any validated network is allowed when wifi is not required`() {
        assertTrue(isEntryDownloadNetworkAllowed(isOnline = true, isWifi = false, requireWifi = false))
        assertFalse(isEntryDownloadNetworkAllowed(isOnline = false, isWifi = true, requireWifi = false))
    }
}
