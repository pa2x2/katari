package eu.kanade.presentation.more.settings.screen.translation.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineState
import mihon.translation.ui.picker.TranslationEnginePickerDensity
import mihon.translation.ui.picker.TranslationEnginePickerList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationEnginePickerContent(
    engines: List<TranslationEngineState>,
    selected: TranslationEngineId,
    onSelect: (TranslationEngineId) -> Unit,
    onOpenSetup: (TranslationEngineId) -> Unit,
    onOpenDocumentation: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(MR.strings.translation_settings_engine),
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        TranslationEnginePickerList(
            engines = engines,
            selected = selected,
            density = TranslationEnginePickerDensity.Full,
            onSelect = onSelect,
            onOpenSetup = onOpenSetup,
            onOpenDocumentation = onOpenDocumentation,
            showMissingSelectionNotice = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

@Composable
internal fun translationEngineLabel(
    engine: TranslationEngineId,
    engines: List<KnownTranslationEngine>,
): String {
    val known = engines.firstOrNull { it.id == engine }
    return known?.engineName
        ?: stringResource(MR.strings.translation_settings_engine_unknown, engine.value)
}
