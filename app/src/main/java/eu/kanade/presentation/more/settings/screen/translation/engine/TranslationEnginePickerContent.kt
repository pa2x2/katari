package eu.kanade.presentation.more.settings.screen.translation.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineId
import mihon.translation.ui.picker.TranslationEnginePickerList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationEnginePickerContent(
    engines: List<KnownTranslationEngine>,
    selected: TranslationEngineId,
    onSelect: (TranslationEngineId) -> Unit,
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
            onSelect = onSelect,
            onOpenDocumentation = onOpenDocumentation,
            showMissingSelectionNotice = true,
            showExplicitPolicyNotice = true,
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
    return known?.let { "${it.engineName} · ${it.providerName}" }
        ?: stringResource(MR.strings.translation_settings_engine_unknown, engine.value)
}
