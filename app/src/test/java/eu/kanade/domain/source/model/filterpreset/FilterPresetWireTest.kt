package eu.kanade.domain.source.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class FilterPresetWireTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `old wire values retain their discriminator while identity is additive`() {
        val old = """[{"type":"select","name":"Type","state":1}]"""
        json.decodeFromString<List<FilterStateNode>>(old) shouldBe listOf(FilterStateNode.Select("Type", 1))
        val nodes = listOf<FilterStateNode>(FilterStateNode.Select("Type", 1, FilterIdentity("type", "movie")))
        json.decodeFromString<List<FilterStateNode>>(json.encodeToString(nodes)) shouldBe nodes
    }

    @Test
    fun `unknown and malformed values survive serialization beside readable values`() {
        val saved = """
            [
              {"type":"future_range","name":"Dates","payload":{"from":2024}},
              {"type":"select","name":"Bad choice","state":"unknown"},
              {"type":"checkbox","name":"Action","state":true}
            ]
        """.trimIndent()
        val decoded = json.decodeFromString<List<FilterStateNode>>(saved)
        (decoded[0] is FilterStateNode.Unknown) shouldBe true
        (decoded[1] is FilterStateNode.Unknown) shouldBe true
        decoded[2] shouldBe FilterStateNode.CheckBox("Action", true)
        json.parseToJsonElement(json.encodeToString(decoded)) shouldBe json.parseToJsonElement(saved)
    }
}
