package eu.kanade.tachiyomi.ui.browse.source.browse.filter.change

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shows how many values changed from the defaults, splitting exclusions out when there are any. */
@Composable
internal fun FilterChangeBadges(change: FilterChange, hasIssues: Boolean) {
    if (!change.isChanged) return
    val description = stringResource(MR.strings.filter_changes_description, change.included, change.excluded)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        if (change.excluded == 0) {
            Badge(
                text = change.changed.toString(),
                tone = if (hasIssues) BadgeTone.Error else BadgeTone.Primary,
            )
        } else {
            if (change.included > 0) {
                Badge(text = "+${change.included}", tone = BadgeTone.Primary)
            }
            Badge(text = "−${change.excluded}", tone = BadgeTone.Error)
        }
    }
}
