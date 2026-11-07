package app.katari.sync.ui.question

import androidx.compose.runtime.Composable
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SyncProblem
import mihon.presentation.kit.surface.NoticeCard
import mihon.presentation.kit.surface.NoticeTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** [question] on its entry's screen, with Choose opening the questions. */
@Composable
fun EntryQuestionBanner(question: EntryQuestion, onChoose: () -> Unit) {
    NoticeCard(
        text = stringResource(
            when (question) {
                EntryQuestion.MIGRATION -> MR.strings.sync_banner_migration
                EntryQuestion.NOTES -> MR.strings.sync_banner_notes
            },
        ),
        tone = NoticeTone.Info,
        icon = MaterialSymbols.Rounded.SyncProblem,
        actionLabel = stringResource(MR.strings.action_choose),
        onAction = onChoose,
    )
}
