package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.Card
import app.katari.presentation.kit.surface.CardStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shown on an entry that belongs to a merge: it points to the merged entry and to managing the merge. */
@Composable
internal fun MergeNotice(
    onOpenMergedEntry: (() -> Unit)?,
    onManageMerge: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        style = CardStyle.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            Text(text = stringResource(MR.strings.merge_member_notice), role = TextRole.Body)
            onOpenMergedEntry?.let {
                Button(
                    label = stringResource(MR.strings.action_open_merged_entry),
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Tonal,
                )
            }
            onManageMerge?.let {
                Button(
                    label = stringResource(MR.strings.action_manage_merge),
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Outlined,
                )
            }
        }
    }
}
