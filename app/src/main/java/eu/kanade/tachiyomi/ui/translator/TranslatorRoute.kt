package eu.kanade.tachiyomi.ui.translator

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.translator.presentation.TranslatorScreenContent
import eu.kanade.tachiyomi.ui.translator.session.TranslatorEvent
import eu.kanade.tachiyomi.ui.translator.session.TranslatorScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslatorRoute(
    screenModel: TranslatorScreenModel,
    onNavigateUp: (() -> Unit)?,
    onOpenSettings: () -> Unit,
) {
    val state by screenModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val speechFailureMessage = stringResource(MR.strings.translator_speech_failed)

    LaunchedEffect(screenModel) {
        screenModel.events.collect { event ->
            when (event) {
                TranslatorEvent.SpeechFailed -> snackbarHostState.showSnackbar(speechFailureMessage)
            }
        }
    }

    TranslatorScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onOpenSettings = onOpenSettings,
        onTextChange = screenModel::setText,
        onClearText = screenModel::clearText,
        onShowPicker = screenModel::showPicker,
        onDismissPicker = screenModel::dismissPicker,
        onSelectAutomaticSource = screenModel::selectAutomaticSource,
        onSelectSource = screenModel::selectSource,
        onSelectTarget = screenModel::selectTarget,
        onSelectEngine = screenModel::selectEngine,
        onSwap = screenModel::swapLanguages,
        onRetry = screenModel::retry,
        onExecute = screenModel::execute,
        onRetryLanguageSupport = screenModel::retryLanguageSupport,
        onSpeechToggle = screenModel::toggleSpeech,
        onExternalAction = { action ->
            screenModel.handleExternalAction(action) { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        },
    )
}
