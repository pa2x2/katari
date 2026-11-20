package app.katari.webview.engine.cookie

import app.katari.core.common.network.cookie.DesktopCookieStore
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class ChromiumCookieMirrorTest {

    @TempDir
    lateinit var directory: Path

    private val store by lazy { DesktopCookieStore(directory) }

    // The store writes its file in the background; one still writing as the test ends races the directory's deletion
    @AfterEach
    fun finishWrites() {
        store.flush()
    }

    // Chromium keeps persistent cookies in its profile on disk; were they kept as it starts, a cookie the app's store
    // removed ("Clear cookies", a server's deletion, a logout) would come back from Chromium into the store.
    @Test
    fun `a cookie Chromium kept from an earlier run doesn't come back after the store removed it`() {
        val site = "https://example.org/".toHttpUrl()
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

    // Chromium holds the store's cookies without their expiries, so one the store let expire stayed in Chromium while
    // only pages ran, and their requests sent it: an expired Anubis pass sent before the new one repeated the challenge.
    @Test
    fun `a cookie that expires leaves Chromium at the next read`() {
        val pass = Cookie.Builder()
            .name("pass")
            .value("1")
            .hostOnlyDomain("example.org")
            .expiresAt(System.currentTimeMillis() + EXPIRY_MS)
            .build()
        store.save(listOf(pass))
        val chromium = FakeChromiumCookies()
        val mirror = ChromiumCookieMirror(store)
        mirror.attach(chromium).get(5, TimeUnit.SECONDS)
        chromium.held.map { it.name } shouldBe listOf("pass")

        Thread.sleep(EXPIRY_MS + 500)
        mirror.readBack()

        runBlocking { eventually(5.seconds) { chromium.held shouldBe emptyList() } }
    }

    private companion object {
        const val EXPIRY_MS = 2_000L
    }
}
