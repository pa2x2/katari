package tachiyomi.domain.entry.service

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState

class EntryLibraryProgressResolver(
    calculators: Collection<EntryLibraryProgressCalculator>,
) {
    private val calculators = calculators.associateBy { it.entryType }

    init {
        check(calculators.size == this.calculators.size) {
            "Duplicate library progress calculators: ${calculators.groupingBy { it.entryType }.eachCount()}"
        }
    }

    suspend fun resolve(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long = 0L,
    ): EntryLibraryState {
        return calculatorFor(entry.type).calculate(entry, chapters, lastRead)
    }

    fun merge(
        entryType: EntryType,
        members: List<LibraryItem>,
    ): EntryLibraryState {
        return calculatorFor(entryType).merge(members)
    }

    private fun calculatorFor(entryType: EntryType): EntryLibraryProgressCalculator {
        return calculators.getValue(entryType)
    }
}

data class EntryLibraryState(
    val progress: ProgressState,
    val lastRead: Long,
    val continueEntryId: Long?,
)

interface EntryLibraryProgressCalculator {
    val entryType: EntryType

    suspend fun calculate(
        entry: Entry,
        chapters: List<EntryChapter>,
        lastRead: Long,
    ): EntryLibraryState

    fun merge(members: List<LibraryItem>): EntryLibraryState
}
