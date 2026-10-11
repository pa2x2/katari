package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.clickableNoIndication

@Composable
internal fun MergedEntriesLink(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickableNoIndication(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = MaterialTheme.colorScheme.primary
        Icon(
            imageVector = EntryActionIcons.merge,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color,
        )
        Text(
            text = pluralStringResource(MR.plurals.entry_merged_entries_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color,
        )
    }
}
