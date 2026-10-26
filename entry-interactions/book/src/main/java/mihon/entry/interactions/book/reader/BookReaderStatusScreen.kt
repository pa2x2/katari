package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.presentation.kit.feedback.InfoScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookReaderLoadingScreen(contentDescription: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {
        LoadingScreen(
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            },
        )
    }
}

@Composable
internal fun BookReaderErrorScreen(
    title: String,
    message: String,
    closeLabel: String,
    onRetry: (() -> Unit)?,
    onClose: () -> Unit,
    onOpenWebView: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {
        InfoScreen(
            icon = MaterialSymbols.Rounded.Error,
            headingText = title,
            subtitleText = message,
            acceptText = if (onRetry != null) stringResource(MR.strings.action_retry) else closeLabel,
            onAcceptClick = onRetry ?: onClose,
            rejectText = closeLabel.takeIf { onRetry != null },
            onRejectClick = onClose.takeIf { onRetry != null },
            alternativeText = stringResource(MR.strings.action_web_view).takeIf { onOpenWebView != null },
            onAlternativeClick = onOpenWebView,
            content = {},
        )
    }
}

@Composable
internal fun BookReaderDialogBackground() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {}
}
