package app.katari.entry.viewer.chrome.ui.mode

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetActions
import app.katari.presentation.kit.sheet.SheetHeader
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A reader mode picker: the choices in [content], applied with Apply; [onUseDefault] goes back to the default. */
@Composable
fun ModeSelectionSheet(
    title: String,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    onUseDefault: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest) {
        Column {
            SheetHeader(title = title)
            content()
            SheetActions(
                confirm = DialogAction(stringResource(MR.strings.action_apply), onApply),
                neutral = onUseDefault?.let { DialogAction(stringResource(MR.strings.action_revert_to_default), it) },
            )
        }
    }
}
