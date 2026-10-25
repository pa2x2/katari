package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class MergeEditorEntry(
    val id: Long,
    val entry: Entry,
    val subtitle: String,
    val isRemovable: Boolean = false,
    val isMember: Boolean = false,
    /** Supplied by the merged entry's source, so it can't be moved, removed or made the root. */
    val locked: Boolean = false,
    val titleOverride: String? = null,
    val coverData: Any? = entry,
) {
    val title: String
        get() = titleOverride ?: entry.displayTitle
}

/** Where a merge candidate comes from, and whether it already belongs to a merge. */
@Composable
internal fun mergeEditorSubtitle(entry: MergeEditorEntry): String = if (entry.isMember) {
    "${entry.subtitle} • ${stringResource(MR.strings.label_member)}"
} else {
    entry.subtitle
}
