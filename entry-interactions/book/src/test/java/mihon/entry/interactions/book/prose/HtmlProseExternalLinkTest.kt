package mihon.entry.interactions.book.prose

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class HtmlProseExternalLinkTest {
    @Test
    fun `launcher accepts retained HTTP schemes case-insensitively`() {
        val uri = "HTTPS://example.com/chapter".toValidatedProseExternalUri()

        assertEquals("https", uri.scheme)
        assertEquals("example.com", uri.host)
    }

    @Test
    fun `launcher still rejects unsupported schemes`() {
        assertFailsWith<IllegalArgumentException> {
            "mailto:reader@example.com".toValidatedProseExternalUri()
        }
    }
}
