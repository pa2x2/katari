package mihon.entry.interactions.book.preparation

import mihon.book.api.BookContentDescriptor

internal class BookContentPreparerRegistry(
    preparers: Collection<BookContentPreparer>,
) {
    private val preparers = preparers.associateBy(BookContentPreparer::id)

    init {
        require(this.preparers.size == preparers.size) {
            "Duplicate BOOK preparer IDs: ${preparers.groupingBy(BookContentPreparer::id).eachCount().duplicates()}"
        }
        require(this.preparers.keys.none(String::isBlank)) { "BOOK preparer IDs must not be blank" }
    }

    fun resolve(descriptor: BookContentDescriptor): BookContentPreparerSelection {
        val compatible = preparers.values.filter { it.supports(descriptor) }
        return when (compatible.size) {
            0 -> BookContentPreparerSelection.Unsupported
            1 -> BookContentPreparerSelection.Selected(compatible.single())
            else -> BookContentPreparerSelection.Ambiguous(compatible.sortedBy(BookContentPreparer::id))
        }
    }
}

internal sealed interface BookContentPreparerSelection {
    data object Unsupported : BookContentPreparerSelection
    data class Selected(val preparer: BookContentPreparer) : BookContentPreparerSelection
    data class Ambiguous(val preparers: List<BookContentPreparer>) : BookContentPreparerSelection
}

private fun Map<String, Int>.duplicates(): String = entries
    .filter { it.value > 1 }
    .joinToString { it.key }
