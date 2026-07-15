package mihon.entry.interactions.book.prose

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HtmlProseChapterReaderWebViewTest {
    @Test
    fun `reader web view disables active and external content capabilities`() {
        val webView = createProseWebView(
            context = RuntimeEnvironment.getApplication(),
            document = "<html><body><p>Chapter</p></body></html>",
            paginated = true,
            initialProgression = 0f,
            onLocation = { _, _, _ -> },
            onTap = {},
            onBoundary = {},
        )

        assertFalse(webView.settings.javaScriptEnabled)
        assertFalse(webView.settings.domStorageEnabled)
        assertFalse(webView.settings.databaseEnabled)
        assertFalse(webView.settings.allowFileAccess)
        assertFalse(webView.settings.allowContentAccess)
        assertTrue(webView.settings.blockNetworkLoads)

        webView.destroy()
    }
}
