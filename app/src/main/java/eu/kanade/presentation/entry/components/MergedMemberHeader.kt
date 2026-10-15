package eu.kanade.presentation.entry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Heads one member's chapters in a merged chapter list; tapping it shows or hides them. */
@Composable
internal fun MergedMemberHeader(
    title: String,
    sourceName: String?,
    childCount: Int,
    unreadCount: Int,
    expanded: Boolean,
    entryType: EntryType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentation = entryType.entryTypePresentation()
    val summary = listOfNotNull(
        sourceName,
        pluralStringResource(presentation.childCountPlural, childCount, childCount),
        unreadCount.takeIf { it > 0 }?.let {
            stringResource(
                MR.strings.entry_member_unconsumed_count,
                stringResource(presentation.filterUnconsumedLabel),
                it,
            )
        },
    ).joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, role = TextRole.Label, tone = ContentTone.Secondary)
            Text(text = summary, role = TextRole.Supporting)
        }
        Icon(
            imageVector = if (expanded) {
                MaterialSymbols.Rounded.KeyboardArrowUp
            } else {
                MaterialSymbols.Rounded.KeyboardArrowDown
            },
            contentDescription = null,
            tone = ContentTone.Secondary,
        )
    }
}
