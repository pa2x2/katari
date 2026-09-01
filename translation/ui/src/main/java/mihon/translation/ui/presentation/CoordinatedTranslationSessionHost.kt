package mihon.translation.ui.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.ui.picker.engine.TranslationEnginePickerList
import mihon.translation.ui.picker.language.TranslationLanguagePairSelector
import mihon.translation.ui.picker.language.TranslationLanguageRole
import mihon.translation.ui.picker.language.TranslationLanguageSupportPicker
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionPicker
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CoordinatedTranslationSessionHost(
    coordinator: TranslationSessionHostCoordinator,
    isTabletUi: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = coordinator.controller::dismiss,
    onPopupBoundsChanged: (Rect?) -> Unit = {},
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
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
        onPopupBoundsChanged = onPopupBoundsChanged,
        speechState = speechState,
        onSpeechToggle = onSpeechToggle,
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
    val languagePair by coordinator.languagePair.collectAsState()
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
                Column {
                    TranslationSessionHeader(
                        title = stringResource(
                            when (picker) {
                                TranslationSessionPicker.LanguagePair ->
                                    MR.strings.translation_change_languages
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
                        TranslationSessionPicker.LanguagePair -> {
                            val canApply = coordinator.canApplyLanguagePair()
                            val supportAvailable = languageSupport is TranslationLanguageSupportState.Available
                            TranslationLanguagePairEditor(
                                sourceLabel = languagePair.source?.displayName()
                                    ?: stringResource(MR.strings.translation_choose_source_language),
                                targetLabel = languagePair.target?.displayName()
                                    ?: stringResource(MR.strings.translation_choose_target_language),
                                canSwap = coordinator.canSwapLanguagePair(),
                                canApply = canApply,
                                showInvalidPair = supportAvailable && !canApply,
                                onChooseSource = {
                                    coordinator.editLanguagePairRole(
                                        TranslationSessionPicker.SourceLanguage,
                                    )
                                },
                                onChooseTarget = {
                                    coordinator.editLanguagePairRole(
                                        TranslationSessionPicker.TargetLanguage,
                                    )
                                },
                                onSwap = coordinator::swapLanguagePair,
                                onApply = coordinator::applyLanguagePair,
                            )
                        }
                        TranslationSessionPicker.SourceLanguage,
                        TranslationSessionPicker.TargetLanguage,
                        -> {
                            val selected = coordinator.selectedLanguage(picker)
                            TranslationLanguageSupportPicker(
                                state = languageSupport,
                                engine = coordinator.activeEngine(),
                                role = when (picker) {
                                    TranslationSessionPicker.SourceLanguage -> TranslationLanguageRole.Source
                                    TranslationSessionPicker.TargetLanguage -> TranslationLanguageRole.Target
                                    TranslationSessionPicker.LanguagePair,
                                    TranslationSessionPicker.Engine,
                                    -> error("Engine picker has no language role")
                                },
                                counterpart = coordinator.counterpartLanguage(picker),
                                selected = selected,
                                onSelect = coordinator::selectLanguage,
                                onRetry = coordinator::retryLanguageSupport,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(top = 8.dp),
                            )
                        }
                        TranslationSessionPicker.Engine -> {
                            TranslationEnginePickerList(
                                engines = engineStates,
                                selected = coordinator.activeEngine(),
                                onSelect = coordinator::selectEngine,
                                selectableOnly = true,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationLanguagePairEditor(
    sourceLabel: String,
    targetLabel: String,
    canSwap: Boolean,
    canApply: Boolean,
    showInvalidPair: Boolean,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwap: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TranslationLanguagePairSelector(
            source = sourceLabel,
            target = targetLabel,
            canSwap = canSwap,
            onChooseSource = onChooseSource,
            onChooseTarget = onChooseTarget,
            onSwap = onSwap,
        )
        if (showInvalidPair) {
            Text(
                text = stringResource(MR.strings.translation_language_pair_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onApply,
            enabled = canApply,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(MR.strings.action_apply))
        }
    }
}

internal val translationSessionDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)
