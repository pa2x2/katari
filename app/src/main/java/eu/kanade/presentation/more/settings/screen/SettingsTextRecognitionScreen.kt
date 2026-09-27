package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.textrecognition.models.ModelArtifactStorageScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.pipeline.TextRecognitionPipelinePickerScreen
import kotlinx.coroutines.flow.map
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsTextRecognitionScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.text_recognition_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val hostActions = remember { Injekt.get<TextRecognitionHostActions>() }
        val modelStore = remember { Injekt.get<ModelArtifactStore>() }
        val storedBytes by remember(modelStore) {
            modelStore.observeStored().map { stored -> stored.sumOf { it.storedBytes } }
        }.collectAsState(initial = null)
        val languages = hostActions.supportedLanguages

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.text_recognition_settings_languages),
                preferenceItems = if (languages.isEmpty()) {
                    listOf(
                        Preference.PreferenceItem.InfoPreference(
                            stringResource(MR.strings.text_recognition_settings_no_languages),
                        ),
                    )
                } else {
                    languages.map { language ->
                        Preference.PreferenceItem.TextPreference(
                            title = language.displayName(),
                            subtitle = pipelineLabel(hostActions, language),
                            isProfileSpecific = true,
                            onClick = { navigator.push(TextRecognitionPipelinePickerScreen(language.value)) },
                        )
                    }
                },
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.text_recognition_settings_models),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.model_artifacts_title),
                        subtitle = storedBytes?.let { formatModelArtifactSize(it) }
                            ?: stringResource(MR.strings.model_artifacts_summary),
                        onClick = { navigator.push(ModelArtifactStorageScreen()) },
                    ),
                ),
            ),
        )
    }

    @Composable
    private fun pipelineLabel(hostActions: TextRecognitionHostActions, language: LanguageTag): String {
        val explicit by remember(hostActions, language) { hostActions.observeSelection(language) }
            .collectAsState(initial = null)
        val selection = explicit
        val default = hostActions.defaultSelection(language)
        return when {
            selection != null -> selectionLabel(hostActions, selection)
            default != null -> stringResource(
                MR.strings.text_recognition_settings_default_pipeline,
                selectionLabel(hostActions, default),
            )
            else -> stringResource(MR.strings.text_recognition_settings_no_pipeline)
        }
    }

    @Composable
    private fun selectionLabel(
        hostActions: TextRecognitionHostActions,
        selection: TextRecognitionPipelineSelection,
    ): String = when (selection) {
        is TextRecognitionPipelineSelection.Preset ->
            hostActions.presets.firstOrNull { it.id == selection.preset }?.displayName ?: selection.preset.value
        is TextRecognitionPipelineSelection.Custom -> stringResource(
            MR.strings.text_recognition_settings_custom_pipeline,
            selection.pipeline.components.joinToString(" + ") { id ->
                hostActions.knownComponents.firstOrNull { it.id == id }?.displayName ?: id.value
            },
        )
    }
}
