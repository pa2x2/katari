package eu.kanade.tachiyomi.ui.browse.source.browse.filter.change

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shows how many values changed from the defaults, splitting exclusions out when there are any. */
@Composable
internal fun FilterChangeBadges(change: FilterChange, hasIssues: Boolean) {
    if (!change.isChanged) return
    val description = stringResource(MR.strings.filter_changes_description, change.included, change.excluded)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (change.excluded == 0) {
            Badge(
                containerColor = if (hasIssues) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (hasIssues) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) { Text(change.changed.toString()) }
        } else {
            if (change.included > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) { Text("+${change.included}") }
            }
            Badge(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) { Text("−${change.excluded}") }
        }
    }
}
