package eu.kanade.tachiyomi.ui.reader.startup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.presentation.kit.feedback.InfoScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ReaderStartupLoadingScreen(step: ReaderStartupStep?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {
        LoadingScreen(message = step?.label())
    }
}

@Composable
private fun ReaderStartupStep.label(): String = when (this) {
    ReaderStartupStep.LoadingExtensions -> stringResource(MR.strings.reader_startup_loading_extensions)
    ReaderStartupStep.OpeningFile -> stringResource(MR.strings.reader_startup_opening_file)
    is ReaderStartupStep.AskingSource -> stringResource(MR.strings.reader_startup_asking_source, sourceName)
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
