package mihon.entry.interactions.book

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.entry.interactions.setEntryInteractionContent
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
            acceptText = closeLabel,
            onAcceptClick = onClose,
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

internal fun FrameLayout.showBookReaderLoading(contentDescription: String) {
    showBookReaderStatus {
        BookReaderLoadingScreen(contentDescription)
    }
}

internal fun FrameLayout.showBookReaderError(
    title: String,
    message: String,
    closeLabel: String,
    onClose: () -> Unit,
) {
    showBookReaderStatus {
        BookReaderErrorScreen(
            title = title,
            message = message,
            closeLabel = closeLabel,
            onClose = onClose,
        )
    }
}

private fun FrameLayout.showBookReaderStatus(content: @Composable () -> Unit) {
    removeAllViews()
    addView(
        ComposeView(context).apply {
            setEntryInteractionContent(content)
        },
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
}
