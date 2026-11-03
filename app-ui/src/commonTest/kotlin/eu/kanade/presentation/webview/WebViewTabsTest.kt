package eu.kanade.presentation.webview

import eu.kanade.tachiyomi.ui.webview.browser.BrowserLoading
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WebViewTabsTest {

    // Share, Open in browser and Clear cookies once acted on whichever page had loaded last, shown or not
    @Test
    fun `a page loading behind the shown tab keeps its address to its own tab`() {
        val tabs = WebViewTabs()
        val opener = FakePage()
        val popup = FakePage()
        tabs.open(opener, "https://example.org/")
        tabs.open(popup, url = null)
        tabs.onUrlChange(popup, "https://example.org/popup")

        tabs.onUrlChange(opener, "https://example.org/next")

        tabs.shown.url shouldBe "https://example.org/popup"
        tabs.closeShown()
        tabs.shown.url shouldBe "https://example.org/next"
        popup.closed shouldBe true
    }

    private class FakePage : BrowserPage {
        var closed = false

        override val title: String? = null
        override val loading: BrowserLoading = BrowserLoading.Finished
        override val canGoBack = false
        override val canGoForward = false

        override fun goBack() = Unit

        override fun goForward() = Unit

        override fun reload() = Unit

        override suspend fun evaluateJavascript(script: String): String? = null

        override fun close() {
            closed = true
        }
    }
}
