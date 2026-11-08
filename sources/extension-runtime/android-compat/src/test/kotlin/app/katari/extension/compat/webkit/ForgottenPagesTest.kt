package app.katari.extension.compat.webkit

import android.content.SharedPreferences
import android.webkit.WebView
import app.katari.core.common.web.ExtensionPage
import app.katari.core.common.web.ExtensionPageEvents
import app.katari.core.common.web.ExtensionPageSettings
import app.katari.core.common.web.ExtensionPageTouch
import app.katari.core.common.web.ExtensionPages
import app.katari.extension.compat.AndroidCompatEnvironment
import app.katari.extension.compat.ExtensionDialogs
import app.katari.extension.compat.SharedPreferencesFiles
import app.katari.extension.compat.ShownDialog
import app.katari.extension.compat.WebViewCookies
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ForgottenPagesTest {

    @TempDir
    lateinit var directory: Path

    // A page keeps its events for as long as it is open, as Chromium's pages do. Events that referred to their view
    // would keep a view its extension dropped, and the Chromium renderer behind it, for the rest of the run, and nothing
    // would show it but processes and memory that never go.
    @Test
    fun `a web view its extension drops without destroying it closes its page once collected`() {
        val pages = OpenPages()
        val environment = environment(pages)
        AndroidCompatEnvironment.install(environment)

        loadAndForget(WebView(environment.context))

        val closed = (1..COLLECTIONS).any {
            System.gc()
            pages.closed.await(WAIT_MILLIS, TimeUnit.MILLISECONDS)
        }
        closed shouldBe true
    }

    private fun loadAndForget(view: WebView) {
        view.loadUrl("https://example.org/")
    }

    private fun environment(pages: ExtensionPages) = AndroidCompatEnvironment(
        dataDir = directory.resolve("data").toFile(),
        cacheDir = directory.resolve("cache").toFile(),
        sharedPreferences = object : SharedPreferencesFiles {
            override fun open(name: String): SharedPreferences = error("No preferences in this test")

            override fun delete(name: String): Boolean = false
        },
        cookies = object : WebViewCookies {
            override fun setCookie(url: String, setCookie: String): Boolean = false

            override fun getCookie(url: String): String? = null

            override fun hasCookies(): Boolean = false

            override fun removeAllCookies(): Boolean = false

            override fun removeSessionCookies(): Boolean = false

            override fun flush() = Unit
        },
        webPages = pages,
        dialogs = object : ExtensionDialogs {
            override fun show(
                page: ExtensionPage,
                title: CharSequence?,
                cancelable: Boolean,
                canceledOnTouchOutside: Boolean,
                onCloseRequest: Runnable,
            ): ShownDialog = error("No dialogs in this test")
        },
        packageName = "app.katari.test",
        versionName = "1",
        versionCode = 1,
        showToast = {},
        isNightMode = { false },
    )

    // Holds each page's events until it is closed, as the host's pages do
    private class OpenPages : ExtensionPages {
        val closed = CountDownLatch(1)
        private val events = mutableListOf<ExtensionPageEvents>()

        override val isOffered = true

        override val defaultUserAgent = "Test"

        override fun open(settings: ExtensionPageSettings, events: ExtensionPageEvents): ExtensionPage {
            this.events += events
            return object : ExtensionPage {
                override fun apply(settings: ExtensionPageSettings) = Unit

                override fun load(url: String, headers: Map<String, String>) = Unit

                override fun loadContent(baseUrl: String?, content: String, mimeType: String, encoding: String) = Unit

                override fun evaluate(script: String, result: ((String) -> Unit)?) = Unit

                override fun addBridge(name: String, methods: Set<String>) = Unit

                override fun removeBridge(name: String) = Unit

                override fun touch(touch: ExtensionPageTouch) = Unit

                override fun stopLoading() = Unit

                override fun reload() = Unit

                override fun close() {
                    closed.countDown()
                }
            }
        }
    }

    private companion object {
        const val COLLECTIONS = 50
        const val WAIT_MILLIS = 100L
    }
}
