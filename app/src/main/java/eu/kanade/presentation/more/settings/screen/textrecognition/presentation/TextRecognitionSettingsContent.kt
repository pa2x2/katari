package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.runtime.Composable
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.more.settings.widget.draft.SettingsDraftSaveBar
import eu.kanade.presentation.more.settings.widget.draft.rememberSettingsDraftLeaveGuard
import kotlinx.coroutines.flow.Flow
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.ui.models.TextRecognitionStoredModels
import mihon.text.recognition.ui.settings.TextRecognitionPlaygroundState
import mihon.text.recognition.ui.settings.TextRecognitionSettingsState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TextRecognitionSettingsContent(
    state: TextRecognitionSettingsState,
    hostActions: TextRecognitionHostActions,
    storedModels: TextRecognitionStoredModels?,
    onBack: (() -> Unit)?,
    onChooseEngine: () -> Unit,
    onChooseLanguages: () -> Unit,
    onOpenModels: () -> Unit,
    onChoosePlaygroundLanguage: () -> Unit,
    onChooseImage: () -> Unit,
    onApprovePlaygroundModels: (List<ModelArtifactDownloadApproval>) -> Unit,
    onApprovePlaygroundPlatformModels: (TextRecognitionPlaygroundState.PlatformModelsRequired) -> Unit,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<Map<ModelArtifactDescriptor, ModelArtifactState>>,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val navigateUp = rememberSettingsDraftLeaveGuard(
        hasUnsavedChanges = state.hasUnsavedProfileChanges,
        saveEnabled = state.hasUnsavedProfileChanges,
        onSave = onSave,
        onDiscard = onDiscard,
        onLeave = onBack,
    )

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(MR.strings.text_recognition_title),
                navigateUp = onBack?.let { navigateUp },
                scrollBehavior = it,
            )
        },
        bottomBar = {
            SettingsDraftSaveBar(
                visible = state.hasUnsavedProfileChanges,
                saveEnabled = state.hasUnsavedProfileChanges,
                onDiscard = onDiscard,
                onSave = onSave,
            )
        },
    ) { contentPadding ->
        PreferenceScreen(
            items = textRecognitionSettingsPreferences(
                engine = state.effectiveProvider?.name
                    ?: stringResource(MR.strings.text_recognition_engine_status_not_included),
                languages = if (state.draft.overrides.isEmpty()) {
                    stringResource(MR.strings.text_recognition_settings_languages_default)
                } else {
                    pluralStringResource(
                        MR.plurals.text_recognition_settings_languages_choices,
                        state.draft.overrides.size,
                        state.draft.overrides.size,
                    )
                },
                storage = storedModels?.let {
                    storageSummary(it)
                } ?: stringResource(MR.strings.model_artifacts_summary),
                onChooseEngine = onChooseEngine,
                onChooseLanguages = onChooseLanguages,
                onOpenModels = onOpenModels,
                playground = {
                    TextRecognitionPlayground(
                        state = state,
                        hostActions = hostActions,
                        onChooseLanguage = onChoosePlaygroundLanguage,
                        onChooseImage = onChooseImage,
                        onApproveModels = onApprovePlaygroundModels,
                        onApprovePlatformModels = onApprovePlaygroundPlatformModels,
                        observeModels = observeModels,
                    )
                },
            ),
            contentPadding = contentPadding,
        )
    }
}

/** Total model storage, and how much of it no engine in this build needs. */
@Composable
private fun storageSummary(models: TextRecognitionStoredModels): String {
    val total = formatModelArtifactSize(models.totalBytes)
    return if (models.obsoleteBytes > 0) {
        stringResource(
            MR.strings.text_recognition_models_storage_summary,
            total,
            formatModelArtifactSize(models.obsoleteBytes),
        )
    } else {
        total
    }
}
