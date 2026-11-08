package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FilterPresetIdentityTest {
    @Test
    fun `stable choices survive relabeling regrouping and option reordering`() {
        val old = PresetChoice().apply { state = 1 }
        val saved = EntryFilterList(presetGroup("Old group", old)).snapshot()
        val updated = PresetChoice(name = "Format", options = listOf("tv", "any", "movie"))
        val current = EntryFilterList(EntryFilter.Header("New heading"), updated)
        current.applySnapshot(saved)
        updated.state shouldBe 2
    }

    @Test
    fun `known legacy labels and historical option indexes migrate across groups`() {
        val old = listOf(FilterStateNode.Group("Old", listOf(FilterStateNode.Select("Language", 2))))
        val updated =
            PresetChoice(
                "Sub Dub",
                "language",
                listOf("dub", "any", "sub"),
                legacyNames = setOf("Language"),
                legacyOptions = listOf("any", "sub", "dub"),
            )
        EntryFilterList(updated).applySnapshot(old)
        updated.state shouldBe 0
        EntryFilterList(updated).snapshot().single().identity shouldBe FilterIdentity("language", "dub")
    }

    @Test
    fun `removed options are reported rather than clamped into another value`() {
        val old = PresetChoice().apply { state = 2 }
        val updated = PresetChoice(options = listOf("any", "movie"))
        val saved = EntryFilterList(old).snapshot()
        val result = EntryFilterList(updated).restoreSnapshot(saved)
        result.issues.single().saved shouldBe saved.single()
        updated.state shouldBe 0
        assertThrows<FilterPresetRepairException> { EntryFilterList(updated).applySnapshot(saved) }
    }

    @Test
    fun `unmodified third party filters restore without metadata`() {
        val filter = object : EntryFilter.Select<String>("Untranslated provider label", arrayOf("A", "B")) {}
        EntryFilterList(filter).applySnapshot(listOf(FilterStateNode.Select(filter.name, 1)))
        filter.state shouldBe 1
    }
}
