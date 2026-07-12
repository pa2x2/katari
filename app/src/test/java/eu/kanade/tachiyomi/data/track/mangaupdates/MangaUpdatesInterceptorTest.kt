package eu.kanade.tachiyomi.data.track.mangaupdates

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test

class MangaUpdatesInterceptorTest {

    @Test
    fun `session is restored for every request after profile switch`() {
        val tracker = mockk<MangaUpdates>()
        every { tracker.restoreSession() } returnsMany listOf("profile-one", "profile-two")
        val requests = mutableListOf<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns Request.Builder().url("https://example.com").build()
        every { chain.proceed(any()) } answers {
            requests += firstArg<Request>()
            mockk<Response>()
        }
        val interceptor = MangaUpdatesInterceptor(tracker)

        interceptor.intercept(chain)
        interceptor.intercept(chain)

        requests.map { it.header("Authorization") } shouldBe listOf(
            "Bearer profile-one",
            "Bearer profile-two",
        )
    }
}
