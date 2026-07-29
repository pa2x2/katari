package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.ui.picker.TranslationLanguageRole
import mihon.translation.ui.picker.TranslationLanguageSupportPicker
import mihon.translation.ui.session.TranslationLanguageSupportState
import tachiyomi.presentation.core.components.material.Scaffold

@Composable
internal fun TranslationLanguagePickerContent(
    title: String,
    support: TranslationLanguageSupportState,
    engine: TranslationEngineId?,
    role: TranslationLanguageRole,
    counterpart: TranslationLanguageTag?,
    selected: TranslationLanguageTag?,
    onSelect: (TranslationLanguageTag?) -> Unit,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}
