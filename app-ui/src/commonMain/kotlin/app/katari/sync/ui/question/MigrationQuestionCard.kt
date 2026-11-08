package app.katari.sync.ui.question

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.SwapHoriz
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.CoverBacking
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.coverFrame
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.list.RowGroupSeparator
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An entry migrated on two devices: one row for each edition it went to, with its cover, title and source, who
 * moved it there and when, and its own Keep. An edition in the profile in use opens on tap.
 */
@Composable
internal fun MigrationQuestionCard(
    question: NamedQuestion.Migration,
    onKeep: (target: String) -> Unit,
    onOpenEntry: (entryId: Long) -> Unit,
) {
    val title = question.entryTitle ?: stringResource(MR.strings.sync_unknown_entry)
    EntryQuestionHeader(
        title = stringResource(MR.strings.sync_question_migration_title, title),
        profile = question.profile,
        text = stringResource(MR.strings.sync_question_migration_kept),
        icon = MaterialSymbols.Rounded.SwapHoriz,
    )
    question.targets.forEachIndexed { index, target ->
        if (index > 0) RowGroupSeparator()
        val entry = target.entry
        CoverRow(
            cover = {
                if (entry != null) {
                    EntryCover.Book(entry.asEntryCover(), Modifier.fillMaxSize(), frame = CoverFrame.Thumbnail)
                } else {
                    Box(Modifier.fillMaxSize().coverFrame(CoverFrame.Thumbnail, CoverBacking.Empty))
                }
            },
            onClick = entry?.takeIf { question.profile?.isActive == true }?.let { { onOpenEntry(it.id) } },
            trailing = {
                Button(
                    label = stringResource(MR.strings.action_keep),
                    onClick = { onKeep(target.key) },
                    style = ButtonStyle.Tonal,
                    size = ButtonSize.Small,
                )
            },
        ) {
            Text(entry?.title ?: title, role = TextRole.ItemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            target.source?.let { Text(it, role = TextRole.Supporting, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            val at = relativeTimeSpanString(target.made.atMillis)
            Text(
                text = when (val on = target.made.on) {
                    MadeOn.ThisDevice -> stringResource(MR.strings.sync_question_migration_moved_here, at)
                    is MadeOn.Other -> stringResource(MR.strings.sync_question_migration_moved, on.inText(), at)
                },
                role = TextRole.Supporting,
            )
        }
    }
}
