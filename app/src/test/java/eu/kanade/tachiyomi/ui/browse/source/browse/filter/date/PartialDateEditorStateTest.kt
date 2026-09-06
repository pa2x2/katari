package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PartialDateEditorStateTest {
    @Test
    fun `browsing historical and future years never supplies a date`() {
        val filter = dateFilter()
        val initial = PartialDateEditorState.initial(filter, 2026)
        val browsing = initial.browseYears(-120).browseYears(240)
        browsing.value shouldBe null
        browsing.canConfirm(filter) shouldBe false
        browsing.chooseYear(1986).candidateText shouldBe "1986"
    }

    @Test
    fun `increasing precision requires explicit missing components without changing original`() {
        val filter = dateFilter(EntryPartialDate(2024))
        val year = PartialDateEditorState.initial(filter)
        year.canConfirm(filter) shouldBe true
        val exact = year.choosePrecision(EntryDatePrecision.DAY)
        exact.missingStep() shouldBe EntryDatePrecision.MONTH
        exact.canConfirm(filter) shouldBe false
        val month = exact.chooseMonth(2)
        month.missingStep() shouldBe EntryDatePrecision.DAY
        month.canConfirm(filter) shouldBe false
        month.chooseDay(29).candidateText shouldBe "2024-02-29"
        filter.state shouldBe "2024"
        PartialDateEditorState.initial(filter).candidateText shouldBe "2024"
    }

    @Test
    fun `lower precision omits subordinate components only from the confirmed candidate`() {
        val filter = dateFilter(EntryPartialDate(2024, 2, 29))
        val month = PartialDateEditorState.initial(filter).choosePrecision(EntryDatePrecision.MONTH)
        month.candidateText shouldBe "2024-02"
        month.choosePrecision(EntryDatePrecision.YEAR).candidateText shouldBe "2024"
        filter.state shouldBe "2024-02-29"
        month.choosePrecision(EntryDatePrecision.DAY).candidateText shouldBe "2024-02-29"
    }

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
    fun `typed precision transfers to picker and disallowed precision remains incomplete`() {
        val filter = dateFilter()
        val typed = PartialDateEditorState.initial(filter).toggleTyping(filter.allowedPrecisions).copy(raw = "1986-4")
        val picker = typed.toggleTyping(filter.allowedPrecisions)
        picker.precision shouldBe EntryDatePrecision.MONTH
        picker.candidateText shouldBe "1986-04"
        val exactOnly = setOf(EntryDatePrecision.DAY)
        val exact = typed.copy(precision = EntryDatePrecision.DAY, raw = "1986").toggleTyping(exactOnly)
        exact.value shouldBe null
        exact.missingStep() shouldBe EntryDatePrecision.MONTH
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
