package eu.kanade.presentation.entry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
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
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (expanded) {
                MaterialSymbols.Rounded.KeyboardArrowUp
            } else {
                MaterialSymbols.Rounded.KeyboardArrowDown
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
