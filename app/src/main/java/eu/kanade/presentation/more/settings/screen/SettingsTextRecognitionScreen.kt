package eu.kanade.presentation.more.settings.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.textrecognition.TextRecognitionSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.textrecognition.engine.TextRecognitionEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.language.TextRecognitionPlaygroundLanguageScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.languages.TextRecognitionLanguagesScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.models.ModelArtifactStorageScreen
import eu.kanade.presentation.more.settings.screen.textrecognition.presentation.TextRecognitionSettingsContent
import eu.kanade.presentation.more.settings.screen.textrecognition.presentation.textRecognitionSettingsPreferences
import eu.kanade.presentation.util.LocalBackPress
import tachiyomi.i18n.*

object SettingsTextRecognitionScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.text_recognition_title

    @Composable
    override fun getPreferences(): List<Preference> = textRecognitionSettingsPreferences()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.current
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()
        val storedModels by model.storedModels.collectAsState()
        val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(model::tryImage)
        }

        TextRecognitionSettingsContent(
            state = state,
            hostActions = model.hostActions,
            storedModels = storedModels,
            onBack = backPress?.let { { it.invoke() } },
            onChooseEngine = { navigator.push(TextRecognitionEnginePickerScreen()) },
            onChooseLanguages = { navigator.push(TextRecognitionLanguagesScreen()) },
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
