package eu.kanade.presentation.more.settings.screen.tts.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.rememberTtsSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.tts.RefreshTtsSettingsOnResume
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.openInBrowser
import mihon.tts.ui.picker.engine.TtsEnginePickerList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsEnginePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsScreenModel()
        val state by model.state.collectAsState()

        RefreshTtsSettingsOnResume(model)
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.tts_settings_choose_engine),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            TtsEnginePickerList(
                inspection = state.engineInspection,
                setupEngines = state.engineInspection.engines
                    .mapTo(mutableSetOf()) { it.engine.id }
                    .filterTo(mutableSetOf(), model::supportsSetup),
                onSelect = { engine ->
                    model.selectDraftEngine(engine)
                    navigator.pop()
                },
                onOpenSetup = { engine ->
                    model.openSetup(engine, context::presentTtsHostActionResult)
                },
                onOpenDocumentation = { context.openInBrowser(it, forceDefaultBrowser = true) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}
