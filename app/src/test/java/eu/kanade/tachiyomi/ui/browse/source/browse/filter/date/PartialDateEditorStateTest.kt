package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PartialDateEditorStateTest {
    @Test
    fun `changing a component clears an impossible day without clamping it`() {
        val leap = PartialDateEditorState.initial(dateFilter(EntryPartialDate(2024, 2, 29)))
        leap.chooseYear(2023).missingStep() shouldBe EntryDatePrecision.DAY
        leap.chooseYear(2023).value shouldBe null
        val january = leap.chooseMonth(1).chooseDay(31)
        january.chooseMonth(4).value shouldBe null
        january.chooseMonth(3).candidateText shouldBe "2024-03-31"
    }

    @Test
    fun `invalid restored text remains visible for repair instead of silently becoming blank`() {
        val filter = dateFilter().also { it.state = "2023-02-29" }
        val state = PartialDateEditorState.initial(filter)
        state.typing shouldBe true
        state.raw shouldBe filter.state
        state.canConfirm(filter) shouldBe false
    }

    private fun dateFilter(value: EntryPartialDate? = null) = EntryDateFilter(
        "Publication date",
        EntryFilterMetadata(id = "publication"),
        initialValue = value,
    )
}
