package mihon.translation.ui.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.ui.presentation.language.TranslationKeptTargetSnackbar
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The translation popup and its pickers for a reader.
 *
 * [snackbarHostState] announces languages the content starts keeping; without it they are kept silently.
 * [onChooseSourceLanguage] replaces the source picker, for readers that own the language their text was read in.
 */
@Composable
fun CoordinatedTranslationSessionHost(
    coordinator: TranslationSessionHostCoordinator,
    isTabletUi: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = coordinator.controller::dismiss,
    onPopupBoundsChanged: (Rect?) -> Unit = {},
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    onChooseSourceLanguage: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val picker by coordinator.picker.collectAsState()
    val languageSuggestions by coordinator.languageSuggestions.collectAsState()
    var latestResult by remember(coordinator) { mutableStateOf<TranslationHostActionResult?>(null) }

    TranslationSessionHost(
        controller = coordinator.controller,
        isTabletUi = isTabletUi,
        onExternalAction = { action ->
            if (action == TranslationSessionExternalAction.ChooseSourceLanguage && onChooseSourceLanguage != null) {
                onChooseSourceLanguage()
            } else {
                coordinator.handleExternalAction(action) { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            }
        },
        modifier = modifier,
        onDismiss = onDismiss,
        onPopupBoundsChanged = onPopupBoundsChanged,
        speechState = speechState,
        onSpeechToggle = onSpeechToggle,
        onSelectSource = coordinator::selectSuggestedSource,
        onSelectTarget = coordinator::selectSuggestedTarget,
        onSelectEngine = coordinator::selectOfferedEngine,
        languageSuggestions = languageSuggestions,
    )
    snackbarHostState?.let { TranslationKeptTargetSnackbar(coordinator, it) }
    LaunchedEffect(coordinator) {
        coordinator.results.collect { result ->
            latestResult = result
        }
    }
    val resultMessage = when (val result = latestResult) {
        null,
        TranslationHostActionResult.Completed,
        -> null
        TranslationHostActionResult.ModelsReady ->
            stringResource(MR.strings.translation_settings_models_ready)
        is TranslationHostActionResult.ModelsFailed -> result.reason
        is TranslationHostActionResult.SetupOpened ->
            when (result.destination) {
                TranslationSetupDestination.InApp -> null
                TranslationSetupDestination.External ->
                    stringResource(MR.strings.translation_settings_external_setup_opened)
            }
        TranslationHostActionResult.SetupUnsupported ->
            stringResource(MR.strings.translation_settings_setup_unsupported)
        TranslationHostActionResult.ServiceMissing ->
            stringResource(MR.strings.translation_service_missing)
        TranslationHostActionResult.SettingsUnavailable ->
            stringResource(MR.strings.translation_settings_unavailable)
        is TranslationHostActionResult.Failed -> result.reason
    }
    LaunchedEffect(resultMessage) {
        resultMessage?.let { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() }
    }
    picker?.let { selectedPicker ->
        TranslationSessionPickerDialog(
            coordinator = coordinator,
            picker = selectedPicker,
            isTabletUi = isTabletUi,
        )
    }
}

internal val translationSessionDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)
