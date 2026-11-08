package app.katari.core.common.network.cookie

import io.kotest.matchers.shouldBe
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DesktopCookieStoreTest {

    @TempDir
    lateinit var directory: Path

    // Android's WebView restores session cookies after a restart; a store that keeps only cookies with an expiry, or
    // reads a host-only cookie back as a domain cookie, logs users out or sends their cookies to every subdomain.
    @Test
    fun `a restart keeps session and host-only cookies as they were and not the ones a server deleted`() {
        val site = "https://example.org/".toHttpUrl()
        val session = Cookie.parse(site, "session=abc; Path=/; HttpOnly")!!
        val remembered = Cookie.parse(site, "remember=yes; Domain=example.org; Max-Age=3600; Secure")!!
        DesktopCookieStore(directory).apply {
            save(listOf(session, remembered, Cookie.parse(site, "gone=1; Path=/")!!))
            save(listOf(Cookie.parse(site, "gone=; Path=/; Max-Age=0")!!))
            flush()
        }

        // The next process's store on the same file
        val reopened = DesktopCookieStore(directory)

        reopened.matching(site).toSet() shouldBe setOf(session, remembered)
        reopened.matching("https://www.example.org/".toHttpUrl()) shouldBe listOf(remembered)
    }
}
