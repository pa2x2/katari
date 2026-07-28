package eu.kanade.presentation.more.settings.screen.translation.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import eu.kanade.presentation.components.AppBar
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.ui.picker.TranslationLanguageOption
import mihon.translation.ui.picker.TranslationLanguagePickerList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

@Composable
internal fun TranslationLanguagePickerContent(
    title: String,
    options: List<TranslationLanguageOption>,
    selected: TranslationLanguageTag?,
    includeAppLanguage: Boolean,
    onSelect: (TranslationLanguageTag?) -> Unit,
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
        TranslationLanguagePickerList(
            options = options,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            defaultOptionLabel = defaultTranslationTargetLabel().takeIf { includeAppLanguage },
            defaultOptionSupporting = stringResource(
                MR.strings.translation_settings_follows_app_language,
            ).takeIf { includeAppLanguage },
            defaultSelected = selected == null,
            onSelectDefault = if (includeAppLanguage) {
                { onSelect(null) }
            } else {
                null
            },
        )
    }
}

@Composable
internal fun defaultTranslationTargetLabel(): String {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
        ?: LocaleListCompat.getAdjustedDefault().get(0)
        ?: Locale.getDefault()
    val name = locale.getDisplayName(Locale.getDefault()).ifBlank { locale.toLanguageTag() }
    return stringResource(MR.strings.translation_settings_target_default, name)
}
