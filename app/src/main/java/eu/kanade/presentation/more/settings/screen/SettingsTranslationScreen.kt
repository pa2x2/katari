package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.translation.engine.TranslationEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import eu.kanade.presentation.more.settings.screen.translation.language.TranslationLanguagePickerScreen
import eu.kanade.presentation.more.settings.screen.translation.language.TranslationLanguagePickerTarget
import eu.kanade.presentation.more.settings.screen.translation.presentation.TranslationSettingsContent
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

object SettingsTranslationScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.translation_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val hostActions = remember { Injekt.get<TranslationHostActions>() }
        val engines = remember(hostActions) { hostActions.knownEngines }
        val engine by hostActions.selectedEngine.collectPreferenceAsState()
        val target by hostActions.defaultTargetLanguage.collectPreferenceAsState()
        val targetSelection = target
        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.translation_settings_playground),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.translation_settings_engine),
                        subtitle = if (hostActions.selectedEngine.isSet()) {
                            translationEngineLabel(engine, engines)
                        } else {
                            stringResource(
                                MR.strings.translation_settings_engine_device_default,
                                translationEngineLabel(engine, engines),
                            )
                        },
                        isProfileSpecific = true,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.translation_settings_target),
                        subtitle = when (targetSelection) {
                            TranslationTargetLanguageSelection.Default ->
                                stringResource(MR.strings.translation_settings_target_app_language)
                            is TranslationTargetLanguageSelection.Explicit ->
                                targetSelection.language.displayName()
                        },
                        isProfileSpecific = true,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.translation_settings_playground),
                        subtitle = stringResource(MR.strings.translation_settings_playground_summary),
                    ),
                ),
            ),
        )
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val backPress = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTranslationSettingsScreenModel()
        val playground by model.playground.collectAsState()
        val engines by model.engines.collectAsState()
        val languageSupport by model.languageSupport.collectAsState()
        val searchHighlightKey = remember { SearchableSettings.highlightKey }

        RefreshTranslationSettingsOnResume(model)
        DisposableEffect(searchHighlightKey) {
            onDispose {
                if (SearchableSettings.highlightKey == searchHighlightKey) {
                    SearchableSettings.highlightKey = null
                }
            }
        }

        fun handleHostResult(result: TranslationHostActionResult) {
            when (result) {
                TranslationHostActionResult.Completed -> model.controller.retry()
                TranslationHostActionResult.ModelsReady -> {
                    context.toast(MR.strings.translation_settings_models_ready)
                    model.controller.retry()
                }
                is TranslationHostActionResult.ModelsFailed -> context.toast(
                    context.stringResource(MR.strings.translation_settings_models_failed, result.reason),
                )
                is TranslationHostActionResult.SetupOpened -> {
                    when (result.destination) {
                        TranslationSetupDestination.InApp -> Unit
                        TranslationSetupDestination.External ->
                            context.toast(MR.strings.translation_settings_external_setup_opened)
                    }
                }
                TranslationHostActionResult.SetupUnsupported ->
                    context.toast(MR.strings.translation_settings_setup_unsupported)
                TranslationHostActionResult.ServiceMissing ->
                    context.toast(MR.strings.translation_service_missing)
                TranslationHostActionResult.SettingsUnavailable ->
                    context.toast(MR.strings.translation_settings_unavailable)
                is TranslationHostActionResult.Failed -> context.toast(
                    context.stringResource(MR.strings.translation_settings_setup_failed, result.reason),
                )
            }
        }

        fun handleExternalAction(action: TranslationSessionExternalAction) {
            when (action) {
                TranslationSessionExternalAction.ChooseSourceLanguage ->
                    navigator.push(
                        TranslationLanguagePickerScreen(
                            TranslationLanguagePickerTarget.PlaygroundSource,
                        ),
                    )
                TranslationSessionExternalAction.ChooseTargetLanguage,
                is TranslationSessionExternalAction.ChangeLanguages,
                -> navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.PlaygroundTarget,
                    ),
                )
                TranslationSessionExternalAction.ChooseEngine ->
                    navigator.push(
                        TranslationEnginePickerScreen(),
                    )
                is TranslationSessionExternalAction.ConfirmProviderDisclosure ->
                    model.acknowledge(action.engine, action.disclosure, ::handleHostResult)
                is TranslationSessionExternalAction.DownloadModels ->
                    model.downloadModels(action.engine, action.models, ::handleHostResult)
                is TranslationSessionExternalAction.OpenSetup ->
                    model.openSetup(action.engine, ::handleHostResult)
                is TranslationSessionExternalAction.OpenDocumentation ->
                    context.openInBrowser(action.url, forceDefaultBrowser = true)
            }
        }

        TranslationSettingsContent(
            playground = playground,
            engines = engines,
            languageSupport = languageSupport,
            controller = model.controller,
            searchHighlightKey = searchHighlightKey,
            onSearchHighlightConsumed = { key ->
                if (SearchableSettings.highlightKey == key) {
                    SearchableSettings.highlightKey = null
                }
            },
            onBack = backPress?.let { { it.invoke() } },
            onTextChange = model::setText,
            onChooseSource = {
                navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.PlaygroundSource,
                    ),
                )
            },
            onChooseTarget = {
                navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.PlaygroundTarget,
                    ),
                )
            },
            onSwapLanguages = model::swapLanguages,
            onChooseEngine = {
                navigator.push(TranslationEnginePickerScreen())
            },
            canOpenSetup = model.supportsSetup(playground.engine),
            onOpenSetup = {
                playground.engine?.let { model.openSetup(it, ::handleHostResult) }
            },
            onSave = {
                model.savePlaygroundDefaults()
                context.toast(MR.strings.translation_settings_saved)
            },
            onExternalAction = ::handleExternalAction,
        )
    }
}

@Composable
internal fun rememberTranslationSettingsScreenModel(): TranslationSettingsScreenModel =
    SettingsTranslationScreen.rememberScreenModel { TranslationSettingsScreenModel() }

@Composable
internal fun RefreshTranslationSettingsOnResume(model: TranslationSettingsScreenModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                model.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
