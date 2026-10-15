package mihon.feature.migration.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.surface.StatusPill
import mihon.presentation.kit.surface.StatusTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How far the background search for replacements has got, and whether it runs or is paused. */
@Composable
internal fun SourceMigrationDiscoveryProgress(
    completed: Int,
    total: Int,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), style = CardStyle.Elevated) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(MR.strings.sourceMigration_findingReplacements),
                        role = TextRole.SectionTitle,
                    )
                    Text(
                        text = stringResource(MR.strings.sourceMigrationReview_discoveryChecked, completed, total),
                        role = TextRole.Supporting,
                    )
                }
                if (paused) {
                    StatusPill(
                        text = stringResource(MR.strings.sourceMigration_discoveryPaused),
                        tone = StatusTone.ActionRequired,
                    )
                } else {
                    StatusPill(
                        text = stringResource(MR.strings.sourceMigrationReview_running),
                        tone = StatusTone.Checking,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { fraction(completed, total) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(MR.strings.sourceMigrationReview_discoveryBackground),
                role = TextRole.Supporting,
            )
        }
    }
}

/** How many of the included entries have been migrated so far. */
@Composable
internal fun SourceMigrationExecutionProgress(completed: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        Text(
            text = stringResource(MR.strings.sourceMigrationReview_migrationProgress, completed, total),
            role = TextRole.SectionTitle,
        )
        LinearProgressIndicator(
            progress = { fraction(completed, total) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun fraction(completed: Int, total: Int): Float = if (total == 0) 0f else completed.toFloat() / total
