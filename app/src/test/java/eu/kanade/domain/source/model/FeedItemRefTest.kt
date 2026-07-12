package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class FeedItemRefTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `decodes legacy manga and anime ref payloads`() {
        json.decodeFromString<FeedItemRef>("""{"type":"manga","id":1}""") shouldBe
            FeedItemRef(1L, EntryType.MANGA)
        json.decodeFromString<FeedItemRef>("""{"type":"anime","id":2}""") shouldBe
            FeedItemRef(2L, EntryType.ANIME)
    }

    @Test
    fun `encodes new refs with entry type`() {
        val encoded = json.encodeToString(FeedItemRef(3L, EntryType.ANIME))
        val parsed = json.parseToJsonElement(encoded).jsonObject

        parsed["id"]?.jsonPrimitive?.content shouldBe "3"
        parsed["type"]?.jsonPrimitive?.content shouldBe "ANIME"
    }
}
