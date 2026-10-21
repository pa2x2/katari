package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.MarkerLine
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.presentation.core.i18n.pluralStringResource

/** Marks a gap in a chapter list's numbering with how many chapters are missing there. */
@Composable
fun MissingChapterCountListItem(
    count: Int,
    entryType: EntryType,
    modifier: Modifier = Modifier,
) {
    MarkerLine(
        text = pluralStringResource(
            entryType.entryTypePresentation().missingChildCountPlural,
            count = count,
            count,
        ),
        modifier = modifier.padding(
            horizontal = KatariTheme.spacing.contentInset,
            vertical = KatariTheme.spacing.s,
        ),
    )
}

@PreviewLightDark
@Composable
private fun Preview() {
    KatariPreviewTheme {
        Surface {
            MissingChapterCountListItem(count = 42, entryType = EntryType.MANGA)
        }
    }
}
