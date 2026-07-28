package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsScreenModel
import eu.kanade.presentation.util.Screen
import mihon.translation.api.TranslationTargetLanguageSelection
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

internal class TranslationLanguagePickerScreen(
    private val target: TranslationLanguagePickerTarget,
    private val model: TranslationSettingsScreenModel,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val options = remember { translationLanguageOptions() }
        val playground by model.playground.collectAsState()
        val defaultTarget by model.preferences.targetLanguage.collectPreferenceAsState()

        TranslationLanguagePickerContent(
            title = stringResource(
                when (target) {
                    TranslationLanguagePickerTarget.PlaygroundSource ->
                        MR.strings.translation_choose_source_language
                    TranslationLanguagePickerTarget.PlaygroundTarget,
                    TranslationLanguagePickerTarget.ProfileTarget,
                    -> MR.strings.translation_choose_target_language
                },
            ),
            options = options,
            selected = when (target) {
                TranslationLanguagePickerTarget.PlaygroundSource -> playground.sourceLanguage
                TranslationLanguagePickerTarget.PlaygroundTarget -> playground.targetLanguage
                TranslationLanguagePickerTarget.ProfileTarget ->
                    (defaultTarget as? TranslationTargetLanguageSelection.Explicit)?.language
            },
            includeAppLanguage = target == TranslationLanguagePickerTarget.ProfileTarget,
            onSelect = { language ->
                when (target) {
                    TranslationLanguagePickerTarget.PlaygroundSource ->
                        language?.let(model::setSourceLanguage)
                    TranslationLanguagePickerTarget.PlaygroundTarget ->
                        language?.let(model::setTargetLanguage)
                    TranslationLanguagePickerTarget.ProfileTarget ->
                        model.setProfileTarget(language)
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
    ProfileTarget,
}
