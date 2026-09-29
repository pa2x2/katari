package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Listing chip for the filtered search, showing how many filters differ from the source defaults. */
@Composable
internal fun CatalogFilterChip(
    selected: Boolean,
    changedCount: Int,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(MR.strings.action_filter))
                if (changedCount > 0) {
                    val description = stringResource(MR.strings.filter_changed_count_description, changedCount)
                    Badge(
                        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(changedCount.toString())
                    }
                }
            }
        },
    )
}
