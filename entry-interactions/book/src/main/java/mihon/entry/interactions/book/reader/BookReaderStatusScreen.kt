package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.InfoScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
internal fun BookReaderLoadingScreen(contentDescription: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
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
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        InfoScreen(
            icon = Icons.Outlined.ErrorOutline,
            headingText = title,
            subtitleText = message,
            acceptText = if (onRetry != null) stringResource(MR.strings.action_retry) else closeLabel,
            onAcceptClick = onRetry ?: onClose,
            rejectText = closeLabel.takeIf { onRetry != null },
            onRejectClick = onClose.takeIf { onRetry != null },
            content = {},
        )
    }
}

@Composable
internal fun BookReaderDialogBackground() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {}
}
