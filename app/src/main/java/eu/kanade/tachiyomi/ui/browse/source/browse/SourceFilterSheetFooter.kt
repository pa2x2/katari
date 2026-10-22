package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Reset and apply under the filter sheet, with why applying is blocked when it is. */
@Composable
internal fun SourceFilterSheetFooter(
    onReset: () -> Unit,
    resetEnabled: Boolean,
    onApply: () -> Unit,
    applyEnabled: Boolean,
    applyBlockedReason: String? = null,
    applyLabel: String = stringResource(MR.strings.action_apply),
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        applyBlockedReason?.let {
            Text(
                text = it,
                modifier = Modifier.padding(
                    start = KatariTheme.spacing.rowInset,
                    end = KatariTheme.spacing.rowInset,
                    top = KatariTheme.spacing.s,
                ),
                role = TextRole.Supporting,
                tone = ContentTone.Error,
                maxLines = 2,
            )
        }
        SheetActions(
            confirm = DialogAction(applyLabel, onApply, enabled = applyEnabled),
            neutral = DialogAction(stringResource(MR.strings.filter_reset_defaults), onReset, enabled = resetEnabled),
        )
    }
}
