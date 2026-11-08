package eu.kanade.presentation.more.settings.screen.translation.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.translation.api.host.TranslationHostActionResult
import app.katari.translation.api.host.TranslationSetupDestination
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.RefreshTranslationSettingsOnResume
import eu.kanade.presentation.more.settings.screen.rememberTranslationSettingsViewModel
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

internal class TranslationEnginePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val linkOpener = LocalAppUiGraph.current.linkOpener
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTranslationSettingsViewModel()
        val playground by model.playground.collectAsState()
        val engines by model.engines.collectAsState()

        RefreshTranslationSettingsOnResume(model)
        TranslationEnginePickerContent(
            engines = engines,
            selected = playground.engine,
            onSelect = { engine ->
                model.setEngine(engine)
                navigator.pop()
            },
            onOpenSetup = { engine ->
                model.openSetup(engine) { result ->
                    when (result) {
                        TranslationHostActionResult.Completed -> Unit
                        TranslationHostActionResult.ModelsReady ->
                            messages.show(MR.strings.translation_settings_models_ready)
                        is TranslationHostActionResult.ModelsFailed ->
                            messages.show(result.reason)
                        is TranslationHostActionResult.SetupOpened ->
                            when (result.destination) {
                                TranslationSetupDestination.InApp -> Unit
                                TranslationSetupDestination.External ->
                                    messages.show(MR.strings.translation_settings_external_setup_opened)
                            }
                        TranslationHostActionResult.SetupUnsupported ->
                            messages.show(MR.strings.translation_settings_setup_unsupported)
                        TranslationHostActionResult.ServiceMissing ->
                            messages.show(MR.strings.translation_service_missing)
                        TranslationHostActionResult.SettingsUnavailable ->
                            messages.show(MR.strings.translation_settings_unavailable)
                        is TranslationHostActionResult.Failed -> messages.show(
                            context.stringResource(
                                MR.strings.translation_settings_setup_failed,
                                result.reason,
                            ),
                        )
                    }
                }
            },
            onOpenDocumentation = { linkOpener.open(context, it, forceDefaultBrowser = true) },
            onBack = navigator::pop,
        )
    }
}
