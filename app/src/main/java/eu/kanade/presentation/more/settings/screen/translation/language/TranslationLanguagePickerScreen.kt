package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTranslationSettingsScreenModel
import eu.kanade.presentation.util.Screen
import mihon.translation.ui.picker.TranslationLanguageRole
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class TranslationLanguagePickerScreen(
    private val target: TranslationLanguagePickerTarget,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTranslationSettingsScreenModel()
        val playground by model.playground.collectAsState()
        val support by model.languageSupport.collectAsState()

        TranslationLanguagePickerContent(
            title = stringResource(
                when (target) {
                    TranslationLanguagePickerTarget.PlaygroundSource ->
                        MR.strings.translation_choose_source_language
                    TranslationLanguagePickerTarget.PlaygroundTarget ->
                        MR.strings.translation_choose_target_language
                },
            ),
            support = support,
            engine = playground.engine,
            role = when (target) {
                TranslationLanguagePickerTarget.PlaygroundSource -> TranslationLanguageRole.Source
                TranslationLanguagePickerTarget.PlaygroundTarget -> TranslationLanguageRole.Target
            },
            counterpart = when (target) {
                TranslationLanguagePickerTarget.PlaygroundSource -> playground.targetLanguage
                TranslationLanguagePickerTarget.PlaygroundTarget -> playground.sourceLanguage
            },
            selected = when (target) {
                TranslationLanguagePickerTarget.PlaygroundSource -> playground.sourceLanguage
                TranslationLanguagePickerTarget.PlaygroundTarget -> playground.targetLanguage
            },
            onRetry = model::retryLanguageSupport,
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
