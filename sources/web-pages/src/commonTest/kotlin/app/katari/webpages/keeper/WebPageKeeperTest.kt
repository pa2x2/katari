package app.katari.webpages.keeper

import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WebPageKeeperTest {
    private val pages = FakeHostPages()
    private var profile = 1L

    private fun TestScope.keeper(perSource: Int = 2, total: Int = 3) =
        WebPageKeeper(pages, WebPageLimits(perSource, total), { profile }, backgroundScope)

    // A page shows one route: a second call let in would navigate it under the first
    @Test
    fun `a second call for a page waits until the first gives it back`() = runTest {
        val keeper = keeper()
        val source = FakeWebPageSource(1)
        val release = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()

        launch {
            keeper.use(source, "main") {
                order += "first in"
                release.await()
                order += "first out"
            }
        }
        launch { keeper.use(source, "main") { order += "second in" } }
        runCurrent()
        order shouldBe listOf("first in")
        release.complete(Unit)
        runCurrent()

        order shouldBe listOf("first in", "first out", "second in")
        pages.opened shouldHaveSize 1
        source.prepared shouldBe 1
    }

    // What a source put into its page, such as a login from its settings, belongs to one profile
    @Test
    fun `a call for another profile gets a page prepared anew`() = runTest {
        val keeper = keeper()
        val source = FakeWebPageSource(1)

        keeper.use(source, "main") {}
        profile = 2
        keeper.use(source, "main") {}

        source.prepared shouldBe 2
        pages.open shouldHaveSize 1
    }

    @Test
    fun `a page too many replaces an idle page and never one a call has`() = runTest {
        val keeper = keeper(total = 1)
        val first = FakeWebPageSource(1)
        val second = FakeWebPageSource(2)
        val release = CompletableDeferred<Unit>()

        launch { keeper.use(first, "main") { release.await() } }
        val waiting = async { keeper.use(second, "main") { "done" } }
        runCurrent()
        pages.opened shouldHaveSize 1
        pages.opened[0].isClosed shouldBe false

        release.complete(Unit)
        waiting.await() shouldBe "done"
        pages.opened shouldHaveSize 2
        pages.opened[0].isClosed shouldBe true
    }

    @Test
    fun `a call ends when its page is lost and the next gets a new one`() = runTest {
        val keeper = keeper()
        val source = FakeWebPageSource(1)

        val lost = async {
            runCatching { keeper.use(source, "main") { page -> page.awaitSelector("#never", timeout = 10.minutes) } }
        }
        runCurrent()
        pages.opened[0].lose()
        runCurrent()

        (lost.await().exceptionOrNull() is WebPageException) shouldBe true
        currentTime shouldBe 0
        keeper.use(source, "main") {}
        pages.opened shouldHaveSize 2
        pages.opened[0].isClosed shouldBe true
        source.prepared shouldBe 2
    }

    @Test
    fun `a page left alone goes idle then closes, and one in use does neither`() = runTest {
        val keeper = keeper()
        val source = FakeWebPageSource(1)
        val release = CompletableDeferred<Unit>()

        val busy = launch { keeper.use(source, "busy") { release.await() } }
        runCurrent()
        keeper.use(source, "main") {}
        val (held, left) = pages.opened
        advanceTimeBy(11.seconds)
        left.isIdle shouldBe true
        keeper.use(source, "main") {}
        left.isIdle shouldBe false

        advanceTimeBy(6.minutes)
        left.isClosed shouldBe true
        held.isIdle shouldBe false
        held.isClosed shouldBe false
        release.complete(Unit)
        busy.join()
    }

    // The watch has to be on before the action: a site's app can answer a tap before the tap's call returns
    @Test
    fun `a response received while the action runs is the one returned`() = runTest {
        val keeper = keeper()
        val source = FakeWebPageSource(1)

        val response = keeper.use(source, "main") { page ->
            pages.opened[0].onTap = { pages.opened[0].receive("https://site.example/api/chapters", "[1,2]") }
            page.responseTo({ "/api/chapters" in it.url }, timeout = 1.seconds) { page.click("#open") }
        }

        response.body shouldBe "[1,2]"
    }

    // Leaving a screen cancels the source's call at any moment, and the page stays for the next one
    @Test
    fun `a call cancelled in the middle of a tap still lifts the finger`() = runTest {
        val keeper = keeper()
        val source = FakeWebPageSource(1)

        val call = launch { keeper.use(source, "main") { page -> page.click("#open") } }
        advanceTimeBy(FakeHostPage.TAP_HELD / 2)
        pages.opened[0].isFingerDown shouldBe true
        call.cancelAndJoin()

        pages.opened[0].isFingerDown shouldBe false
    }

    @Test
    fun `a source's page too many fails where its others are in use`() = runTest {
        val keeper = keeper(perSource = 1)
        val source = FakeWebPageSource(1)

        shouldThrow<WebPageException> {
            keeper.use(source, "main") { keeper.use(source, "second") {} }
        }
        keeper.use(source, "second") {}
        pages.open shouldHaveSize 1
    }
}
