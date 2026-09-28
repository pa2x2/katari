package eu.kanade.presentation.more.settings.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.textrecognition.TextRecognitionSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.textrecognition.engine.TextRecognitionEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.language.TextRecognitionPlaygroundLanguageScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.models.ModelArtifactStorageScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.overrides.TextRecognitionOverridesScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.presentation.TextRecognitionSettingsContent
import eu.kanade.presentation.util.LocalBackPress
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

object SettingsTextRecognitionScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.text_recognition_title

    @Composable
    override fun getPreferences(): List<Preference> = listOf(
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.text_recognition_settings_playground),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.text_recognition_settings_engine),
                    isProfileSpecific = true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.text_recognition_settings_language_overrides),
                    isProfileSpecific = true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.model_artifacts_title),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.text_recognition_settings_playground),
                    subtitle = stringResource(MR.strings.text_recognition_settings_playground_summary),
                ),
            ),
        ),
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.current
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()
        val storedBytes by model.storedBytes.collectAsState()
        val searchHighlightKey = remember { SearchableSettings.highlightKey }
        val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(model::tryImage)
        }

        DisposableEffect(searchHighlightKey) {
            onDispose {
                if (SearchableSettings.highlightKey == searchHighlightKey) {
                    SearchableSettings.highlightKey = null
                }
            }
        }
        TextRecognitionSettingsContent(
            state = state,
            hostActions = model.hostActions,
            storedBytes = storedBytes,
            searchHighlightKey = searchHighlightKey,
            onSearchHighlightConsumed = { key ->
                if (SearchableSettings.highlightKey == key) {
                    SearchableSettings.highlightKey = null
                }
            },
            onBack = backPress?.let { { it.invoke() } },
            onChooseEngine = { navigator.push(TextRecognitionEnginePickerScreen()) },
            onChooseOverrides = { navigator.push(TextRecognitionOverridesScreen()) },
            onOpenModels = { navigator.push(ModelArtifactStorageScreen()) },
            onChoosePlaygroundLanguage = { navigator.push(TextRecognitionPlaygroundLanguageScreen()) },
            onChooseImage = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onApprovePlaygroundModels = model.controller::approvePlaygroundModels,
            onApprovePlaygroundPlatformModels = model.controller::approvePlaygroundPlatformModels,
            observeModels = model.controller::observeModels,
            onSave = model.controller::save,
            onDiscard = model.controller::discard,
        )
    }
}

@Composable
internal fun rememberTextRecognitionSettingsScreenModel(): TextRecognitionSettingsScreenModel =
    SettingsTextRecognitionScreen.rememberScreenModel { TextRecognitionSettingsScreenModel() }
