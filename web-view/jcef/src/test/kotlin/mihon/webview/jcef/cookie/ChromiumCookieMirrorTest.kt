package mihon.webview.jcef.cookie

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mihon.core.common.network.cookie.DesktopCookieStore
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ChromiumCookieMirrorTest {

    @TempDir
    lateinit var directory: Path

    // Chromium keeps persistent cookies in its profile on disk; were they kept as it starts, a cookie the app's store
    // removed ("Clear cookies", a server's deletion, a logout) would come back from Chromium into the store.
    @Test
    fun `a cookie Chromium kept from an earlier run doesn't come back after the store removed it`() {
        val site = "https://example.org/".toHttpUrl()
        val store = DesktopCookieStore(directory)
        val kept = Cookie.parse(site, "kept=1; Max-Age=3600")!!
        store.save(listOf(kept))
        val chromium = FakeChromiumCookies().apply {
            setByPage(Cookie.parse(site, "cleared=1; Max-Age=3600")!!.toChromiumCookie())
        }
        val mirror = ChromiumCookieMirror(store)

        mirror.attach(chromium).get(5, TimeUnit.SECONDS)

        runBlocking { mirror.cookiesFor(site) } shouldBe listOf(kept)
        chromium.held.map { it.name } shouldBe listOf("kept")
    }
}
