package eu.kanade.presentation.more.settings.screen.textrecognition.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsScreenModel
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Picks the language of the text in the playground image. */
internal class TextRecognitionPlaygroundLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()

        TextRecognitionLanguagePickerContent(
            title = stringResource(MR.strings.text_recognition_settings_text_language),
            languages = state.languages,
            selected = state.playgroundLanguage,
            onSelect = { language ->
                model.setPlaygroundLanguage(language)
                navigator.pop()
            },
            onBack = navigator::pop,
        )
    }
}
