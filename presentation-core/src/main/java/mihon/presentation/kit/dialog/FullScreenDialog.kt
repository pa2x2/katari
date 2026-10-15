package mihon.presentation.kit.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A window over the whole screen, drawn behind the system bars, for a viewer such as a cover shown at full size.
 * [content] lays itself out edge to edge and pads for the bars itself, as a Scaffold does.
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        content = content,
    )
}
