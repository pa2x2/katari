package mihon.translation.ui.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationSetupDestination
import mihon.translation.ui.picker.TranslationEnginePickerDensity
import mihon.translation.ui.picker.TranslationEnginePickerList
import mihon.translation.ui.picker.TranslationLanguageRole
import mihon.translation.ui.picker.TranslationLanguageSupportPicker
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionPicker
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CoordinatedTranslationSessionHost(
    coordinator: TranslationSessionHostCoordinator,
    isTabletUi: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = coordinator.controller::dismiss,
) {
    val context = LocalContext.current
    val picker by coordinator.picker.collectAsState()
    var latestResult by remember(coordinator) { mutableStateOf<TranslationHostActionResult?>(null) }

    TranslationSessionHost(
        controller = coordinator.controller,
        isTabletUi = isTabletUi,
        onExternalAction = { action ->
            coordinator.handleExternalAction(action) { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        },
        modifier = modifier,
        onDismiss = onDismiss,
    )
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

@Composable
private fun TranslationSessionPickerDialog(
    coordinator: TranslationSessionHostCoordinator,
    picker: TranslationSessionPicker,
    isTabletUi: Boolean,
) {
    val context = LocalContext.current
    val engineStates by coordinator.engineStates.collectAsState()
    val languageSupport by coordinator.languageSupport.collectAsState()
    Dialog(
        onDismissRequest = coordinator::dismissPicker,
        properties = translationSessionDialogProperties,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AdaptiveSheet(
                isTabletUi = isTabletUi,
                enableImplicitDismiss = true,
                onDismissRequest = coordinator::dismissPicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.85f),
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    TranslationSessionHeader(
                        title = stringResource(
                            when (picker) {
                                TranslationSessionPicker.SourceLanguage ->
                                    MR.strings.translation_choose_source_language
                                TranslationSessionPicker.TargetLanguage ->
                                    MR.strings.translation_choose_target_language
                                TranslationSessionPicker.Engine -> MR.strings.translation_choose_engine
                            },
                        ),
                        onDismiss = coordinator::dismissPicker,
                    )
                    HorizontalDivider()
                    when (picker) {
                        TranslationSessionPicker.SourceLanguage,
                        TranslationSessionPicker.TargetLanguage,
                        -> {
                            val selected = coordinator.selectedLanguage(picker)
                            if (
                                picker == TranslationSessionPicker.TargetLanguage &&
                                coordinator.canUseCurrentTargetAsProfileDefault()
                            ) {
                                TextButton(onClick = coordinator::useCurrentTargetAsProfileDefault) {
                                    Text(stringResource(MR.strings.translation_settings_use_target_as_default))
                                }
                            }
                            TranslationLanguageSupportPicker(
                                state = languageSupport,
                                engine = coordinator.activeEngine(),
                                role = when (picker) {
                                    TranslationSessionPicker.SourceLanguage -> TranslationLanguageRole.Source
                                    TranslationSessionPicker.TargetLanguage -> TranslationLanguageRole.Target
                                    TranslationSessionPicker.Engine -> error("Engine picker has no language role")
                                },
                                counterpart = coordinator.counterpartLanguage(picker),
                                selected = selected,
                                onSelect = coordinator::selectLanguage,
                                onRetry = coordinator::retryLanguageSupport,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 8.dp),
                            )
                        }
                        TranslationSessionPicker.Engine -> {
                            TranslationEnginePickerList(
                                engines = engineStates,
                                selected = coordinator.activeEngine(),
                                density = TranslationEnginePickerDensity.Compact,
                                onSelect = coordinator::selectEngine,
                                onOpenSetup = coordinator::openEngineSetup,
                                onOpenDocumentation = { url ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal val translationSessionDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)
