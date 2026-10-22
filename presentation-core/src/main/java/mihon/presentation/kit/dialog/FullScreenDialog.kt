package mihon.presentation.kit.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mihon.presentation.kit.surface.MediaBackdrop

/**
 * A window over the whole screen, drawn behind the system bars, for a viewer such as a cover shown at full size. It is
 * opaque, on the media backdrop, so the screen it opened from doesn't show around the media. [content] lays itself out
 * edge to edge and pads for the bars itself, as a Scaffold does.
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
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaBackdrop(modifier = Modifier.fillMaxSize())
            content()
        }
    }
}
