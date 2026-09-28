package eu.kanade.presentation.more.settings.screen.textrecognition.language

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.picker.language.TranslationLanguagePickerList
import mihon.translation.ui.picker.language.translationLanguageOptionsOf
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/** A full-screen, searchable choice among the languages text recognition can read, like Translation's pickers. */
@Composable
internal fun TextRecognitionLanguagePickerContent(
    title: String,
    languages: List<LanguageTag>,
    selected: LanguageTag?,
    suggested: List<LanguageTag>,
    onSelect: (LanguageTag) -> Unit,
    onBack: () -> Unit,
) {
    val options = remember(languages) { translationLanguageOptionsOf(languages) }
    val suggestedOptions = remember(suggested, options) {
        suggested.mapNotNull { language -> options.firstOrNull { it.tag == language } }
    }
    Scaffold(
        topBar = {
            AppBar(
                title = title,
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        TranslationLanguagePickerList(
            options = options,
            selected = selected,
            onSelect = onSelect,
            shortcuts = suggestedOptions,
            shortcutsTitle = stringResource(MR.strings.text_recognition_languages_suggested),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}
