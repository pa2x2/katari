package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The titles hidden from this recap by hand, each with a way to bring it back. */
@Composable
internal fun RecapHiddenTitlesDialog(
    titles: List<StatisticsRecapTitle>,
    onShowAgain: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.statistics_recap_hidden_titles),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismiss),
    ) {
        if (titles.isEmpty()) {
            Text(stringResource(MR.strings.statistics_recap_no_hidden_titles))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
                titles.forEach { title ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            label = stringResource(MR.strings.statistics_recap_show_again),
                            onClick = { onShowAgain(title.entryId) },
                            style = ButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}
