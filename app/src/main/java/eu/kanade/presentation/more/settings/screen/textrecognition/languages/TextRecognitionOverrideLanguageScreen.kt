package eu.kanade.presentation.more.settings.screen.textrecognition.languages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.textrecognition.language.TextRecognitionLanguagePickerContent
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Picks a language without an override, among [languageTags] when given, then continues to its pipeline choice.
 */
internal class TextRecognitionOverrideLanguageScreen(
    private val languageTags: List<String>? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()
        val suggested by model.suggestedLanguages.collectAsState()
        val overridden = state.draft.overrides.keys.map { it.value.substringBefore('-') }.toSet()

        TextRecognitionLanguagePickerContent(
            title = stringResource(MR.strings.text_recognition_settings_choose_language),
            languages = state.languages
                .filter { languageTags == null || it.value in languageTags }
                .filterNot { it.value.substringBefore('-') in overridden },
            selected = null,
            suggested = suggested,
            onSelect = { language -> navigator.replace(TextRecognitionOverridePipelineScreen(language.value)) },
            onBack = navigator::pop,
        )
    }
}
