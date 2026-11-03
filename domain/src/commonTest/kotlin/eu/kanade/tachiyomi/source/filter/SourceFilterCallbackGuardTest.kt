package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterGroupSummary
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterStateSemantics
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidator
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourceFilterCallbackGuardTest {
    private class BrokenBound : EntryFilter.Text("Minimum"), EntryFilterStateSemantics {
        override fun activeSelectionCount(value: EntryFilter<*>): Int? = error("broken count")
        override val canClearSelection: Boolean get() = error("broken clear capability")
        override fun clearSelection(value: EntryFilter<*>) = error("broken clear")
    }

    private class BrokenGroup :
        EntryFilter.Group<EntryFilter<*>>("Chapters", listOf(BrokenBound())),
        EntryFilterValidator,
        EntryFilterGroupSummary {
        override fun validateFilter(values: List<EntryFilter<*>>): List<EntryFilterValidationIssue> =
            error("broken validation")
        override fun selectionSummary(values: List<EntryFilter<*>>): String? = throw IndexOutOfBoundsException()
    }

    @Test
    fun `throwing extension callbacks degrade the filter instead of crashing the editor`() {
        val group = EntryFilterList(BrokenGroup()).detachedCopy().single() as EntryFilter.Group<*>
        val bound = group.state.single() as EntryFilter<*>

        (group as EntryFilterGroupSummary).selectionSummary(group.state.filterIsInstance<EntryFilter<*>>()) shouldBe
            null
        group.validationIssues() shouldBe emptyList()
        val semantics = bound.sourceStateSemantics()!!
        semantics.activeSelectionCount(bound) shouldBe null
        semantics.canClearSelection shouldBe false
        semantics.clearSelection(bound)

        group.hasFailedSourceCallback() shouldBe true
        bound.hasFailedSourceCallback() shouldBe true
    }
}
