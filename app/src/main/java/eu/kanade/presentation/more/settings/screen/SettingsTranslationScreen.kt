package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.translation.TranslationHostActionResult
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.translation.engine.TranslationEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.translation.engine.TranslationEnginePickerTarget
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import eu.kanade.presentation.more.settings.screen.translation.language.TranslationLanguagePickerScreen
import eu.kanade.presentation.more.settings.screen.translation.language.TranslationLanguagePickerTarget
import eu.kanade.presentation.more.settings.screen.translation.language.displayName
import eu.kanade.presentation.more.settings.screen.translation.presentation.TranslationSettingsContent
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.runtime.ProfileTranslationPreferences
import mihon.translation.spi.KnownTranslationEngineCatalog
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
        val preferences = remember { Injekt.get<ProfileTranslationPreferences>() }
        val engines = remember { Injekt.get<KnownTranslationEngineCatalog>().knownEngines }
        val engine by preferences.engine.collectPreferenceAsState()
        val target by preferences.targetLanguage.collectPreferenceAsState()
        val targetSelection = target
        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.translation_settings_defaults),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.translation_settings_engine),
                        subtitle = translationEngineLabel(engine, engines),
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
        val lifecycleOwner = LocalLifecycleOwner.current
        val backPress = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { TranslationSettingsScreenModel() }
        val playground by model.playground.collectAsState()
        val defaultEngine by model.preferences.engine.collectPreferenceAsState()
        val defaultTarget by model.preferences.targetLanguage.collectPreferenceAsState()
        var retryAfterResume by remember { mutableStateOf(false) }

        DisposableEffect(lifecycleOwner, model) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && retryAfterResume) {
                    retryAfterResume = false
                    model.controller.retry()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                TranslationHostActionResult.SystemSetupOpened -> {
                    retryAfterResume = true
                    context.toast(MR.strings.translation_settings_setup_opened)
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
                            model,
                        ),
                    )
                TranslationSessionExternalAction.ChooseTargetLanguage,
                is TranslationSessionExternalAction.ChangeLanguages,
                -> navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.PlaygroundTarget,
                        model,
                    ),
                )
                TranslationSessionExternalAction.ChooseEngine ->
                    navigator.push(
                        TranslationEnginePickerScreen(
                            TranslationEnginePickerTarget.Playground,
                            model,
                        ),
                    )
                is TranslationSessionExternalAction.UseTargetAsDefault -> {
                    model.setDefaultTarget(action.language)
                    context.toast(MR.strings.translation_settings_default_updated)
                }
                is TranslationSessionExternalAction.ConfirmProviderDisclosure ->
                    model.acknowledge(action.engine, action.disclosure, ::handleHostResult)
                is TranslationSessionExternalAction.DownloadModels ->
                    model.downloadModels(action.engine, action.models, ::handleHostResult)
                is TranslationSessionExternalAction.OpenSystemSetup ->
                    model.openSystemSetup(action.engine, ::handleHostResult)
                is TranslationSessionExternalAction.OpenDocumentation ->
                    context.openInBrowser(action.url, forceDefaultBrowser = true)
            }
        }

        TranslationSettingsContent(
            playground = playground,
            engines = model.engines,
            defaultEngine = defaultEngine,
            defaultTarget = defaultTarget,
            controller = model.controller,
            onBack = backPress?.let { { it.invoke() } },
            onTextChange = model::setText,
            onChooseSource = {
                navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.PlaygroundSource,
                        model,
                    ),
                )
            },
            onChooseTarget = {
                navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.PlaygroundTarget,
                        model,
                    ),
                )
            },
            onSwapLanguages = model::swapLanguages,
            onChooseEngine = {
                navigator.push(
                    TranslationEnginePickerScreen(
                        TranslationEnginePickerTarget.Playground,
                        model,
                    ),
                )
            },
            onChooseDefaultEngine = {
                navigator.push(
                    TranslationEnginePickerScreen(
                        TranslationEnginePickerTarget.Profile,
                        model,
                    ),
                )
            },
            onChooseDefaultTarget = {
                navigator.push(
                    TranslationLanguagePickerScreen(
                        TranslationLanguagePickerTarget.ProfileTarget,
                        model,
                    ),
                )
            },
            onUseEngineAsDefault = {
                model.usePlaygroundEngineAsDefault()
                context.toast(MR.strings.translation_settings_default_engine_updated)
            },
            onExternalAction = ::handleExternalAction,
        )
    }
}
