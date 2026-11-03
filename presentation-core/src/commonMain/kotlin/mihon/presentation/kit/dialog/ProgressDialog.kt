package mihon.presentation.kit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme

/**
 * Blocks the screen while work the user waits for runs: [message] next to the loading indicator, or above a bar once
 * [progress] (0..1) is known. Only [cancel] closes it; Back and taps outside do nothing, since the work keeps running
 * underneath. The caller closes it when the work ends.
 */
@Composable
fun ProgressDialog(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    progress: (() -> Float)? = null,
    cancel: DialogAction? = null,
) {
    Dialog(
        onDismissRequest = {},
        title = title,
        confirm = null,
        modifier = modifier,
        dismiss = cancel,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        val spacing = KatariTheme.spacing
        if (progress == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingIndicator()
                Text(message)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.l)) {
                Text(message)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = progress)
            }
        }
    }
}
