package mihon.translation.ui.presentation

import android.content.ClipData
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Embedded Translation session renderer for hosts that own their surrounding controls.
 *
 * Preparation, setup, progress, failure, execution, and result content is shared with [TranslationSessionHost].
 */
@Composable
fun TranslationSessionPanel(
    controller: TranslationSessionController,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    modifier: Modifier = Modifier,
    showCopy: Boolean = true,
) {
    val state by controller.state.collectAsState()
    val active = state as? TranslationSessionState.Active ?: return
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipboardLabel = stringResource(MR.strings.translation_title)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        TranslationSessionContent(
            state = active,
            expanded = true,
            showHeader = false,
            showExpand = false,
            showLanguageChange = false,
            showCopy = showCopy,
            useExternalEnginePicker = true,
            onDismiss = controller::dismiss,
            onExecute = controller::execute,
            onRetry = controller::retry,
            onCopy = { text ->
                scope.launch {
                    clipboard.setClipEntry(
                        ClipData.newPlainText(clipboardLabel, text).toClipEntry(),
                    )
                }
            },
            onExpand = {},
            onSelectSource = controller::selectSourceLanguage,
            onSelectEngine = controller::selectEngine,
            onExternalAction = onExternalAction,
        )
    }
}
