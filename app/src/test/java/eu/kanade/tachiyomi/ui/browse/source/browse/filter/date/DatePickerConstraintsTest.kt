package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationCode
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DatePickerConstraintsTest {
    @Test
    fun `exact-only bounded picker offers parent periods but requires an in-range day`() {
        val filter = EntryDateFilter(
            "Published on",
            EntryFilterMetadata(id = "published"),
            allowedPrecisions = setOf(EntryDatePrecision.DAY),
            required = true,
            minimum = EntryPartialDate(2024, 2, 10),
            maximum = EntryPartialDate(2024, 3, 20),
        )
        val state = PartialDateEditorState.initial(filter, 2026)
        state.precision shouldBe EntryDatePrecision.DAY
        state.value shouldBe null
        filter.containsPeriod(EntryPartialDate(2024)) shouldBe true
        filter.containsPeriod(EntryPartialDate(2024, 1)) shouldBe false
        filter.containsPeriod(EntryPartialDate(2024, 2)) shouldBe true
        val february = state.chooseYear(2024).chooseMonth(2)
        february.canConfirm(filter) shouldBe false
        february.chooseDay(9).canConfirm(filter) shouldBe false
        february.chooseDay(10).canConfirm(filter) shouldBe true
        february.chooseMonth(3).chooseDay(20).canConfirm(filter) shouldBe true
        february.chooseMonth(3).chooseDay(21).canConfirm(filter) shouldBe false
    }

    @Test
    fun `month-only picker completes at month and optional clearing is distinct from an incomplete choice`() {
        val filter = EntryDateFilter(
            "Issue month",
            EntryFilterMetadata(id = "issue"),
            allowedPrecisions = setOf(EntryDatePrecision.MONTH),
        )
        val state = PartialDateEditorState.initial(filter)
        state.canConfirm(filter) shouldBe false
        state.chooseYear(1986).canConfirm(filter) shouldBe false
        state.chooseYear(1986).chooseMonth(4).canConfirm(filter) shouldBe true
        state.toggleTyping(filter.allowedPrecisions).canConfirm(filter) shouldBe true
        val required = EntryDateFilter(
            "Issue month",
            filter.filterMetadata,
            allowedPrecisions = filter.allowedPrecisions,
            required = true,
        )
        state.toggleTyping(required.allowedPrecisions).canConfirm(required) shouldBe false
    }

    @Test
    fun `source validation sees detached typed candidate while live draft remains unchanged`() {
        val filter = object : EntryDateFilter("Publication", EntryFilterMetadata(id = "date")) {
            override fun validateFilter(values: List<EntryFilter<*>>): List<EntryFilterValidationIssue> {
                val candidate = values.singleOrNull() as? EntryDateFilter ?: this
                return super.validateFilter(values) + if (candidate.dateValue?.year == 2024) {
                    listOf(
                        EntryFilterValidationIssue(setOf("date"), EntryFilterValidationCode.SOURCE, "Unavailable year"),
                    )
                } else {
                    emptyList()
                }
            }
        }
        filter.state = "2023"
        val state = PartialDateEditorState.initial(filter).chooseYear(2024)
        state.canConfirm(filter) shouldBe false
        state.validationIssues(filter).single().message shouldBe "Unavailable year"
        filter.state shouldBe "2023"
    }
}
