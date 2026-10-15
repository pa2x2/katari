package eu.kanade.tachiyomi.ui.translator

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.translator.presentation.TranslatorScreenContent
import eu.kanade.tachiyomi.ui.translator.session.TranslatorEvent
import eu.kanade.tachiyomi.ui.translator.session.TranslatorViewModel
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslatorRoute(
    viewModel: TranslatorViewModel,
    onNavigateUp: (() -> Unit)?,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val speechFailureMessage = stringResource(MR.strings.translator_speech_failed)
    val swapUnavailableMessage = stringResource(MR.strings.translator_swap_unavailable)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TranslatorEvent.SpeechFailed -> snackbarHostState.showSnackbar(speechFailureMessage)
                TranslatorEvent.SwapUnavailable -> snackbarHostState.showSnackbar(swapUnavailableMessage)
            }
        }
    }

    TranslatorScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onOpenSettings = onOpenSettings,
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
        onSpeechToggle = viewModel::toggleSpeech,
        onExternalAction = { action ->
            viewModel.handleExternalAction(action) { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        },
    )
}
