package eu.kanade.tachiyomi.ui.translator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.navigation.AppBar
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.ui.translator.presentation.TranslatorScreenContent
import eu.kanade.tachiyomi.ui.translator.session.TranslatorEvent
import eu.kanade.tachiyomi.ui.translator.session.TranslatorViewModel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslatorRoute(
    viewModel: TranslatorViewModel,
    onNavigateUp: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    syncAction: AppBar.Action?,
) {
    val state by viewModel.state.collectAsState()
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val linkOpener = graph.linkOpener
    val snackbarHostState = remember { SnackbarHostState() }
    val speechFailureMessage = stringResource(MR.strings.translator_speech_failed)
    val swapUnavailableMessage = stringResource(MR.strings.translator_swap_unavailable)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TranslatorEvent.SpeechFailed ->
                    snackbarHostState.showSnackbar(event.explanation ?: speechFailureMessage)
                TranslatorEvent.SwapUnavailable -> snackbarHostState.showSnackbar(swapUnavailableMessage)
            }
        }
    }

    TranslatorScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onOpenSettings = onOpenSettings,
        syncAction = syncAction,
        onTextChange = viewModel::setText,
        onClearText = viewModel::clearText,
        onShowPicker = viewModel::showPicker,
        onDismissPicker = viewModel::dismissPicker,
        onSelectAutomaticSource = viewModel::selectAutomaticSource,
        onSelectSource = viewModel::selectSource,
        onSelectTarget = viewModel::selectTarget,
        onSelectDefaultTarget = viewModel::selectDefaultTarget,
        onSelectEngine = viewModel::selectEngine,
        onSwap = viewModel::swapLanguages,
        onRetry = viewModel::retry,
        onExecute = viewModel::execute,
        onRetryLanguageSupport = viewModel::retryLanguageSupport,
        onSpeechToggle = viewModel::toggleSpeech.takeIf { graph.ttsHostActions.isOffered },
        onExternalAction = { action ->
            viewModel.handleExternalAction(action) { url -> linkOpener.open(context, url) }
        },
    )
}
