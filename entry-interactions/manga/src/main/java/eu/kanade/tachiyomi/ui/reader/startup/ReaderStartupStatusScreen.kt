package eu.kanade.tachiyomi.ui.reader.startup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.InfoScreen
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
internal fun ReaderStartupLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LoadingScreen()
    }
}

@Composable
internal fun ReaderStartupErrorScreen(
    failure: ReaderStartupState.Failed,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        InfoScreen(
            icon = Icons.Outlined.ErrorOutline,
            headingText = stringResource(MR.strings.chapter_error),
            subtitleText = failure.message,
            acceptText = if (failure.canRetry) {
                stringResource(MR.strings.action_retry)
            } else {
                stringResource(MR.strings.action_close)
            },
            onAcceptClick = if (failure.canRetry) onRetry else onClose,
            rejectText = stringResource(MR.strings.action_close).takeIf { failure.canRetry },
            onRejectClick = onClose.takeIf { failure.canRetry },
            content = {},
        )
    }
}
