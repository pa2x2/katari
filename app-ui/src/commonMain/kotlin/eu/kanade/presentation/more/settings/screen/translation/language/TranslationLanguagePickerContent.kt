package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.ui.picker.language.TranslationLanguageRole
import app.katari.translation.ui.picker.language.TranslationLanguageSupportPicker
import app.katari.translation.ui.session.TranslationLanguageSupportState

@Composable
internal fun TranslationLanguagePickerContent(
    title: String,
    support: TranslationLanguageSupportState,
    engine: TranslationEngineId?,
    engineName: String?,
    role: TranslationLanguageRole,
    counterpart: LanguageTag?,
    selected: LanguageTag?,
    onSelect: (LanguageTag?) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = title,
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        TranslationLanguageSupportPicker(
            state = support,
            engine = engine,
            role = role,
            counterpart = counterpart,
            selected = selected,
            onSelect = onSelect,
            onRetry = onRetry,
            engineName = engineName,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}
