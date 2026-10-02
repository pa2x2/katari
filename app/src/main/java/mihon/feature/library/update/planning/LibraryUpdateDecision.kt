package mihon.feature.library.update.planning

import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason

sealed interface LibraryUpdateDecision {
    val item: LibraryItem

    data class Check(
        override val item: LibraryItem,
        /** Sources whose members of a merged entry stay unchecked because their source is switched off. */
        val skippedSourceIds: Set<Long> = emptySet(),
    ) : LibraryUpdateDecision

    data class Leave(
        override val item: LibraryItem,
        val reason: EntryUpdateDecisionReason,
        /** The category whose switch, rules or interval decided, when one did. */
        val categoryId: Long? = null,
    ) : LibraryUpdateDecision
}
