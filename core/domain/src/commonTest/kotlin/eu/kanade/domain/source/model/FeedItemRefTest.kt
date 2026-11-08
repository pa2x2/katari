package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class FeedItemRefTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `persisted lowercase and current entry ref payloads both decode`() {
        json.decodeFromString<FeedItemRef>("""{"type":"manga","id":1}""") shouldBe
            FeedItemRef(1L, EntryType.MANGA)
        val current = FeedItemRef(2L, EntryType.ANIME)
        json.decodeFromString<FeedItemRef>(json.encodeToString(current)) shouldBe current
    }
}
