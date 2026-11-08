package mihon.presentation.kit.dialog

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * The app's [Dialog] around a web page that something other than the app puts up, such as an extension's check that
 * needs a person: [page] fills the content area, as tall as the dialog is wide, since a page has no height of its own.
 * The user can close it only where it has a [dismiss] action, then also by back or Esc and, with
 * [dismissOnClickOutside], by clicking outside it.
 */
@Composable
fun WebPageDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    dismiss: DialogAction?,
    dismissOnClickOutside: Boolean,
    page: @Composable (Modifier) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = null,
        dismiss = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismiss != null,
            dismissOnClickOutside = dismiss != null && dismissOnClickOutside,
        ),
    ) {
        page(Modifier.fillMaxWidth().aspectRatio(1f))
    }
}
