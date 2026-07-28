package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsScreenModel
import eu.kanade.presentation.util.Screen
import mihon.translation.ui.picker.translationLanguageOptions
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class TranslationLanguagePickerScreen(
    private val target: TranslationLanguagePickerTarget,
    private val model: TranslationSettingsScreenModel,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val options = remember { translationLanguageOptions() }
        val playground by model.playground.collectAsState()

        TranslationLanguagePickerContent(
            title = stringResource(
                when (target) {
                    TranslationLanguagePickerTarget.PlaygroundSource ->
                        MR.strings.translation_choose_source_language
                    TranslationLanguagePickerTarget.PlaygroundTarget ->
                        MR.strings.translation_choose_target_language
                },
            ),
            options = options,
            selected = when (target) {
                TranslationLanguagePickerTarget.PlaygroundSource -> playground.sourceLanguage
                TranslationLanguagePickerTarget.PlaygroundTarget -> playground.targetLanguage
            },
            includeAppLanguage = false,
            onSelect = { language ->
                when (target) {
                    TranslationLanguagePickerTarget.PlaygroundSource ->
                        language?.let(model::setSourceLanguage)
                    TranslationLanguagePickerTarget.PlaygroundTarget ->
                        language?.let(model::setTargetLanguage)
                }
                navigator.pop()
            },
            onBack = navigator::pop,
        )
    }
}

internal enum class TranslationLanguagePickerTarget {
    PlaygroundSource,
    PlaygroundTarget,
}
