package eu.kanade.presentation.more.settings.screen.textrecognition.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsViewModel
import eu.kanade.presentation.util.Screen
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.text.recognition.ui.picker.engine.TextRecognitionEnginePickerList
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class TextRecognitionEnginePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val linkOpener = LocalAppUiGraph.current.linkOpener
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsViewModel()
        val state by model.state.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.text_recognition_settings_choose_engine),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            TextRecognitionEnginePickerList(
                providers = state.providers,
                components = model.hostActions.knownComponents,
                selected = state.effectiveProvider?.id,
                onSelect = { provider ->
                    model.controller.selectDraftEngine(provider)
                    navigator.pop()
                },
                onOpenDocumentation = { linkOpener.open(context, it, forceDefaultBrowser = true) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}
