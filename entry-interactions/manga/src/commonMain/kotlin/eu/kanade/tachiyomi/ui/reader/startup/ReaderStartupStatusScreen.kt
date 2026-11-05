package eu.kanade.tachiyomi.ui.reader.startup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.presentation.kit.feedback.InfoScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ReaderStartupLoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
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
        container = SurfaceContainer.Background,
    ) {
        InfoScreen(
            icon = MaterialSymbols.Rounded.Error,
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
