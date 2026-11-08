package app.katari.sync.ui.question

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.SyncProblem
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
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
