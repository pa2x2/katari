package eu.kanade.presentation.more.settings.screen.textrecognition.overrides

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsScreenModel
import eu.kanade.presentation.util.Screen
import mihon.text.recognition.ui.picker.language.TextRecognitionLanguagePickerList
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/** Picks a language without an override, then continues to its pipeline choice. */
internal class TextRecognitionOverrideLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()
        val overridden = state.draft.overrides.keys.map { it.value.substringBefore('-') }.toSet()

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.text_recognition_settings_choose_language),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            TextRecognitionLanguagePickerList(
                languages = state.languages.filterNot { it.value.substringBefore('-') in overridden },
                onSelect = { language -> navigator.replace(TextRecognitionOverridePipelineScreen(language.value)) },
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }
    }
}
