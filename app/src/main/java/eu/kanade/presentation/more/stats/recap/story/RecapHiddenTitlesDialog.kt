package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The titles hidden from this recap by hand, each with a way to bring it back. */
@Composable
internal fun RecapHiddenTitlesDialog(
    titles: List<StatisticsRecapTitle>,
    onShowAgain: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.statistics_recap_hidden_titles)) },
        text = {
            if (titles.isEmpty()) {
                Text(stringResource(MR.strings.statistics_recap_no_hidden_titles))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    titles.forEach { title ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onShowAgain(title.entryId) }) {
                                Text(stringResource(MR.strings.statistics_recap_show_again))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_ok)) }
        },
    )
}
