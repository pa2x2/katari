package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.SettingsRow
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
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.statistics_recap_hidden_titles),
        confirm = DialogAction(stringResource(MR.strings.action_done), onDismiss),
    ) {
        if (titles.isEmpty()) {
            Text(stringResource(MR.strings.statistics_recap_no_hidden_titles))
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                titles.forEach { title ->
                    SettingsRow(
                        title = title.title,
                        trailing = {
                            Button(
                                label = stringResource(MR.strings.statistics_recap_show_again),
                                onClick = { onShowAgain(title.entryId) },
                                style = ButtonStyle.Text,
                            )
                        },
                    )
                }
            }
        }
    }
}
