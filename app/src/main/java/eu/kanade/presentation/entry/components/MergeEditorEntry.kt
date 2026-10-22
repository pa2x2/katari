package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class MergeEditorEntry(
    val id: Long,
    val entry: Entry,
    val subtitle: String,
    val isRemovable: Boolean = false,
    val isMember: Boolean = false,
    val titleOverride: String? = null,
    val coverData: Any? = entry,
) {
    val title: String
        get() = titleOverride ?: entry.displayTitle
}

@Composable
internal fun MergeEditorEntryCover(entry: MergeEditorEntry) {
    EntryCover.Square(
        data = entry.coverData,
        modifier = Modifier.size(KatariTheme.sizes.artwork),
        frame = CoverFrame.Thumbnail,
    )
}

/**
 * The title and source line of a merge candidate. [subtitleTone] lets a marked row's container colour its text;
 * [extra] adds lines below, such as what the row is marked for.
 */
@Composable
internal fun MergeEditorEntryDetails(
    entry: MergeEditorEntry,
    modifier: Modifier = Modifier,
    subtitleTone: ContentTone = ContentTone.Inherit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Text(
            text = entry.title,
            role = TextRole.Body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = mergeEditorSubtitle(entry),
            role = TextRole.Supporting,
            tone = subtitleTone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        extra()
    }
}

/** Where a merge candidate comes from, and whether it already belongs to a merge. */
@Composable
internal fun mergeEditorSubtitle(entry: MergeEditorEntry): String = if (entry.isMember) {
    "${entry.subtitle} • ${stringResource(MR.strings.label_member)}"
} else {
    entry.subtitle
}
