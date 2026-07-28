package mihon.translation.ui.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.TranslationEnginePickerList
import mihon.translation.ui.picker.TranslationLanguagePickerList
import mihon.translation.ui.picker.translationLanguageOptions
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
        TranslationHostActionResult.SystemSetupOpened ->
            stringResource(MR.strings.translation_settings_setup_opened)
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
        TranslationSessionPickerDialog(coordinator, selectedPicker)
    }
}

@Composable
private fun TranslationSessionPickerDialog(
    coordinator: TranslationSessionHostCoordinator,
    picker: TranslationSessionPicker,
) {
    val state by coordinator.controller.state.collectAsState()
    val active = state as? TranslationSessionState.Active
    val options = remember { translationLanguageOptions() }
    Dialog(onDismissRequest = coordinator::dismissPicker) {
        BoxWithConstraints {
            AdaptiveSheet(
                isTabletUi = maxWidth >= 720.dp,
                enableImplicitDismiss = true,
                onDismissRequest = coordinator::dismissPicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.85f),
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    Text(
                        text = stringResource(
                            when (picker) {
                                TranslationSessionPicker.SourceLanguage ->
                                    MR.strings.translation_choose_source_language
                                TranslationSessionPicker.TargetLanguage ->
                                    MR.strings.translation_choose_target_language
                                TranslationSessionPicker.Engine -> MR.strings.translation_choose_engine
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HorizontalDivider()
                    when (picker) {
                        TranslationSessionPicker.SourceLanguage,
                        TranslationSessionPicker.TargetLanguage,
                        -> {
                            val sourceSelection =
                                active?.input?.request?.sourceLanguage as? TranslationSourceLanguageSelection.Explicit
                            val targetSelection =
                                active?.input?.request?.targetLanguage as? TranslationTargetLanguageSelection.Explicit
                            val selected = when (picker) {
                                TranslationSessionPicker.SourceLanguage -> sourceSelection?.language
                                TranslationSessionPicker.TargetLanguage -> targetSelection?.language
                                TranslationSessionPicker.Engine -> null
                            }
                            if (picker == TranslationSessionPicker.TargetLanguage && selected != null) {
                                TextButton(onClick = coordinator::useCurrentTargetAsProfileDefault) {
                                    Text(stringResource(MR.strings.translation_settings_use_target_as_default))
                                }
                            }
                            TranslationLanguagePickerList(
                                options = options,
                                selected = selected,
                                onSelect = coordinator::selectLanguage,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TranslationSessionPicker.Engine -> {
                            val selected = when (
                                val selection = active?.input?.request?.engine
                            ) {
                                is TranslationEngineSelection.Explicit -> selection.engine
                                TranslationEngineSelection.ProfileDefault,
                                null,
                                -> coordinator.profileSelectedEngine
                            }
                            TranslationEnginePickerList(
                                engines = coordinator.knownEngines,
                                selected = selected,
                                onSelect = coordinator::selectEngine,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
