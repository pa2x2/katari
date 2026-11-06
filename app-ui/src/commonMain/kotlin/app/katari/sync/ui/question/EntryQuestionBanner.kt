package app.katari.sync.ui.question

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SyncProblem
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** [question] on its entry's screen, with Choose opening the questions. */
@Composable
fun EntryQuestionBanner(question: EntryQuestion, onChoose: () -> Unit) {
    RowGroup(
        listOf<@Composable () -> Unit> {
            SettingsRow(
                title = stringResource(
                    when (question) {
                        EntryQuestion.MIGRATION -> MR.strings.sync_banner_migration
                        EntryQuestion.NOTES -> MR.strings.sync_banner_notes
                    },
                ),
                subtitle = stringResource(
                    when (question) {
                        EntryQuestion.MIGRATION -> MR.strings.sync_banner_migration_text
                        EntryQuestion.NOTES -> MR.strings.sync_banner_notes_text
                    },
                ),
                icon = MaterialSymbols.Rounded.SyncProblem,
                trailing = {
                    Button(
                        label = stringResource(MR.strings.action_choose),
                        onClick = onChoose,
                        style = ButtonStyle.Tonal,
                        size = ButtonSize.Small,
                    )
                },
                onClick = onChoose,
            )
        },
    )
}
