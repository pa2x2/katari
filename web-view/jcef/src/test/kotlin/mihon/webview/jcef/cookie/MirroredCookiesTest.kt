package mihon.webview.jcef.cookie

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mihon.core.common.network.cookie.CookieChange
import mihon.core.common.network.cookie.DesktopCookieStore
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MirroredCookiesTest {

    @TempDir
    lateinit var directory: Path

    private val site = "https://example.org/".toHttpUrl()

    // JCEF carries no SameSite and Chromium rounds and caps expiries, so a store that took back what Chromium reports
    // would lose a cookie's SameSite, cut its expiry, and set the poorer copy in Chromium again on the next change.
    @Test
    fun `a round trip through Chromium keeps what JCEF can't carry and sets nothing back`() {
        val store = DesktopCookieStore(directory)
        val chromium = FakeChromiumCookies()
        val mirrored = MirroredCookies()
        val cookies = listOf(
            Cookie.parse(site, "token=a; Max-Age=86400000; Secure; HttpOnly; SameSite=None")!!,
            Cookie.parse(site, "session=b; SameSite=Strict")!!,
            Cookie.parse(site, "pref=c; Domain=example.org; Path=/; Max-Age=3600")!!,
        )
        store.save(cookies)
        cookies.forEach { mirrored.storeChanged(CookieChange.Stored(it), chromium) }

        mirrored.read(runBlocking { chromium.read() }, store)

        store.all() shouldContainExactlyInAnyOrder cookies
        chromium.held.map { it.domain } shouldContainExactlyInAnyOrder
            listOf("example.org", "example.org", ".example.org")

        chromium.setByPage(ChromiumCookie("visited", "1", "example.org", "/", false, false, null))
        mirrored.read(runBlocking { chromium.read() }, store)
        val visited = store.all().single { it.name == "visited" }
        val setsBefore = chromium.sets

        mirrored.storeChanged(CookieChange.Stored(visited), chromium) shouldBe false
        chromium.sets shouldBe setsBefore
    }

    // Reads come late: a page's change found by a read can be older than a request's change to the same cookie, and a
    // cookie Chromium refuses is missing from it without anyone having deleted it.
    @Test
    fun `Chromium's changes reach the store only where Chromium made them and the store hasn't moved on`() {
        val store = DesktopCookieStore(directory)
        val chromium = FakeChromiumCookies()
        val mirrored = MirroredCookies()
        val login = Cookie.parse(site, "login=1; Max-Age=3600")!!
        val theme = Cookie.parse(site, "theme=dark; Max-Age=3600")!!
        val refused = Cookie.parse(site, "__Host-id=7; Path=/")!!
        store.save(listOf(login, theme, refused))
        listOf(login, theme, refused).forEach { mirrored.storeChanged(CookieChange.Stored(it), chromium) }
        mirrored.read(runBlocking { chromium.read() }, store)

        // The page logs out and changes the theme, while a response changes the theme too, not yet passed on
        chromium.remove(login.toChromiumCookie())
        chromium.setByPage(theme.toChromiumCookie().copy(value = "light"))
        val newerTheme = Cookie.parse(site, "theme=sepia; Max-Age=3600")!!
        store.save(listOf(newerTheme))
        mirrored.read(runBlocking { chromium.read() }, store)
        mirrored.storeChanged(CookieChange.Stored(newerTheme), chromium)

        store.all() shouldContainExactlyInAnyOrder listOf(newerTheme, refused)
        chromium.held.map { it.name to it.value } shouldBe listOf("theme" to "sepia")
    }
}
