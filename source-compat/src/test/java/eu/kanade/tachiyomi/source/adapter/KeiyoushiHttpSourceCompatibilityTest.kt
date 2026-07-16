package eu.kanade.tachiyomi.source.adapter

import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import okhttp3.Headers
import org.junit.jupiter.api.Test

class KeiyoushiHttpSourceCompatibilityTest {

    @Test
    fun `legacy HttpSource keeps its upstream class layout`() {
        HttpSource::class.java.superclass shouldBe Any::class.java
        HttpSource::class.java.declaredFields.map { it.name } shouldContainAll listOf(
            "network\$delegate",
            "id\$delegate",
            "headers\$delegate",
        )
    }

    @Test
    fun `extensions-lib 1_6 can replace the HttpSource headers delegate`() {
        val source = ReflectiveHeadersSource()

        source.headers["X-Build"] shouldBe "1"
        source.headers["X-Build"] shouldBe "2"
    }
}

private class ReflectiveHeadersSource : HttpSource() {
    override val name: String = "Reflective headers source"
    override val lang: String = "en"
    override val baseUrl: String = "https://example.invalid"
    override val supportsLatest: Boolean = false

    private var headersBuildCount = 0

    init {
        val delegate = object : Lazy<Headers> {
            override val value: Headers get() = headersBuilder().build()
            override fun isInitialized(): Boolean = true
        }

        HttpSource::class.java.getDeclaredField("headers\$delegate").apply {
            isAccessible = true
            set(this@ReflectiveHeadersSource, delegate)
        }
    }

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("X-Build", (++headersBuildCount).toString())
}
