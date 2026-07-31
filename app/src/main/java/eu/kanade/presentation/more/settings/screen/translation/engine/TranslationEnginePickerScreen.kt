package eu.kanade.presentation.more.settings.screen.translation.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.RefreshTranslationSettingsOnResume
import eu.kanade.presentation.more.settings.screen.rememberTranslationSettingsScreenModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationSetupDestination
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

internal class TranslationEnginePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTranslationSettingsScreenModel()
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
                            context.toast(MR.strings.translation_settings_models_ready)
                        is TranslationHostActionResult.ModelsFailed ->
                            context.toast(result.reason)
                        is TranslationHostActionResult.SetupOpened ->
                            when (result.destination) {
                                TranslationSetupDestination.InApp -> Unit
                                TranslationSetupDestination.External ->
                                    context.toast(MR.strings.translation_settings_external_setup_opened)
                            }
                        TranslationHostActionResult.SetupUnsupported ->
                            context.toast(MR.strings.translation_settings_setup_unsupported)
                        TranslationHostActionResult.ServiceMissing ->
                            context.toast(MR.strings.translation_service_missing)
                        TranslationHostActionResult.SettingsUnavailable ->
                            context.toast(MR.strings.translation_settings_unavailable)
                        is TranslationHostActionResult.Failed -> context.toast(
                            context.stringResource(
                                MR.strings.translation_settings_setup_failed,
                                result.reason,
                            ),
                        )
                    }
                }
            },
            onOpenDocumentation = { context.openInBrowser(it, forceDefaultBrowser = true) },
            onBack = navigator::pop,
        )
    }
}
