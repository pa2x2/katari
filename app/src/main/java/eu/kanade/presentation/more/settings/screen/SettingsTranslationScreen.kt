package eu.kanade.presentation.more.settings.screen

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.model.rememberScreenModel
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.screen.translation.TranslationHostActionResult
import eu.kanade.presentation.more.settings.screen.translation.TranslationLanguagePairDialog
import eu.kanade.presentation.more.settings.screen.translation.TranslationLanguagePickerDialog
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.translation.TranslationTestInputDialog
import eu.kanade.presentation.more.settings.screen.translation.displayName
import eu.kanade.presentation.more.settings.screen.translation.translationLanguageOptions
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.runtime.ProfileTranslationPreferences
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationSessionHost
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

object SettingsTranslationScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.translation_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = remember { Injekt.get<ProfileTranslationPreferences>() }
        val engines = remember { Injekt.get<KnownTranslationEngineCatalog>().knownEngines }
        return translationPreferences(
            preferences = preferences,
            engines = engines,
            onChooseTarget = {},
            onTest = {},
            onOpenDocumentation = {},
        )
    }

    @Composable
    override fun Content() {
        val backPress = LocalBackPress.current
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val model = rememberScreenModel { TranslationSettingsScreenModel() }
        val languages = remember { translationLanguageOptions() }
        var showTestInput by remember { mutableStateOf(false) }
        var languageDialog by remember { mutableStateOf<TranslationLanguageDialog?>(null) }
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
                is TranslationHostActionResult.ModelsFailed ->
                    context.toast(
                        context.stringResource(
                            MR.strings.translation_settings_models_failed,
                            result.reason,
                        ),
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
                is TranslationHostActionResult.Failed ->
                    context.toast(
                        context.stringResource(
                            MR.strings.translation_settings_setup_failed,
                            result.reason,
                        ),
                    )
            }
        }

        fun handleExternalAction(action: TranslationSessionExternalAction) {
            when (action) {
                TranslationSessionExternalAction.ChooseSourceLanguage -> {
                    languageDialog = TranslationLanguageDialog.RequestSource
                }
                TranslationSessionExternalAction.ChooseTargetLanguage -> {
                    languageDialog = TranslationLanguageDialog.RequestTarget
                }
                is TranslationSessionExternalAction.ChangeLanguages -> {
                    languageDialog = TranslationLanguageDialog.Pair(
                        source = action.source,
                        target = action.target,
                    )
                }
                is TranslationSessionExternalAction.UseTargetAsDefault -> {
                    model.setDefaultTarget(action.language)
                    context.toast(MR.strings.translation_settings_default_updated)
                }
                is TranslationSessionExternalAction.ConfirmProviderDisclosure -> {
                    model.acknowledge(action.engine, action.disclosure, ::handleHostResult)
                }
                is TranslationSessionExternalAction.DownloadModels -> {
                    model.downloadModels(action.engine, action.models, ::handleHostResult)
                }
                is TranslationSessionExternalAction.OpenSystemSetup -> {
                    model.openSystemSetup(action.engine, ::handleHostResult)
                }
                is TranslationSessionExternalAction.OpenDocumentation -> {
                    context.openInBrowser(action.url, forceDefaultBrowser = true)
                }
            }
        }

        val items = translationPreferences(
            preferences = model.preferences,
            engines = model.engines,
            onChooseTarget = { languageDialog = TranslationLanguageDialog.DefaultTarget },
            onTest = { showTestInput = true },
            onOpenDocumentation = { context.openInBrowser(it, forceDefaultBrowser = true) },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            PreferenceScaffold(
                titleRes = MR.strings.translation_title,
                onBackPressed = backPress?.let { { it.invoke() } },
                itemsProvider = { items },
            )
            TranslationSessionHost(
                controller = model.controller,
                isTabletUi = isTabletUi(),
                onExternalAction = ::handleExternalAction,
            )
        }

        if (showTestInput) {
            TranslationTestInputDialog(
                onSubmit = { text ->
                    showTestInput = false
                    model.submitTest(text)
                },
                onDismissRequest = { showTestInput = false },
            )
        }

        when (val dialog = languageDialog) {
            null -> Unit
            TranslationLanguageDialog.DefaultTarget -> {
                val current = model.preferences.targetLanguage.get()
                TranslationLanguagePickerDialog(
                    title = stringResource(MR.strings.translation_settings_target),
                    options = languages,
                    selected = (current as? TranslationTargetLanguageSelection.Explicit)?.language,
                    includeAppLanguage = true,
                    appLanguageLabel = defaultTargetLabel(),
                    onSelect = { language ->
                        model.setDefaultTarget(language)
                        languageDialog = null
                    },
                    onDismissRequest = { languageDialog = null },
                )
            }
            TranslationLanguageDialog.RequestSource -> {
                TranslationLanguagePickerDialog(
                    title = stringResource(MR.strings.translation_choose_source_language),
                    options = languages,
                    selected = null,
                    includeAppLanguage = false,
                    appLanguageLabel = "",
                    onSelect = { language ->
                        language ?: return@TranslationLanguagePickerDialog
                        model.controller.selectSourceLanguage(language)
                        languageDialog = null
                    },
                    onDismissRequest = { languageDialog = null },
                )
            }
            TranslationLanguageDialog.RequestTarget -> {
                TranslationLanguagePickerDialog(
                    title = stringResource(MR.strings.translation_choose_target_language),
                    options = languages,
                    selected = null,
                    includeAppLanguage = false,
                    appLanguageLabel = "",
                    onSelect = { language ->
                        language ?: return@TranslationLanguagePickerDialog
                        model.controller.selectTargetLanguage(language)
                        languageDialog = null
                    },
                    onDismissRequest = { languageDialog = null },
                )
            }
            is TranslationLanguageDialog.Pair -> {
                TranslationLanguagePairDialog(
                    source = dialog.source,
                    target = dialog.target,
                    onChooseSource = {
                        languageDialog = TranslationLanguageDialog.PairPicker(
                            pair = dialog,
                            field = TranslationLanguageField.Source,
                        )
                    },
                    onChooseTarget = {
                        languageDialog = TranslationLanguageDialog.PairPicker(
                            pair = dialog,
                            field = TranslationLanguageField.Target,
                        )
                    },
                    onConfirm = {
                        model.controller.selectSourceLanguage(dialog.source)
                        model.controller.selectTargetLanguage(dialog.target)
                        languageDialog = null
                    },
                    onDismissRequest = { languageDialog = null },
                )
            }
            is TranslationLanguageDialog.PairPicker -> {
                val selected = when (dialog.field) {
                    TranslationLanguageField.Source -> dialog.pair.source
                    TranslationLanguageField.Target -> dialog.pair.target
                }
                TranslationLanguagePickerDialog(
                    title = stringResource(
                        when (dialog.field) {
                            TranslationLanguageField.Source ->
                                MR.strings.translation_choose_source_language
                            TranslationLanguageField.Target ->
                                MR.strings.translation_choose_target_language
                        },
                    ),
                    options = languages,
                    selected = selected,
                    includeAppLanguage = false,
                    appLanguageLabel = "",
                    onSelect = { language ->
                        language ?: return@TranslationLanguagePickerDialog
                        languageDialog = when (dialog.field) {
                            TranslationLanguageField.Source -> dialog.pair.copy(source = language)
                            TranslationLanguageField.Target -> dialog.pair.copy(target = language)
                        }
                    },
                    onDismissRequest = { languageDialog = dialog.pair },
                )
            }
        }
    }
}

@Composable
private fun translationPreferences(
    preferences: ProfileTranslationPreferences,
    engines: List<KnownTranslationEngine>,
    onChooseTarget: () -> Unit,
    onTest: () -> Unit,
    onOpenDocumentation: (String) -> Unit,
): List<Preference> {
    val target by preferences.targetLanguage.collectAsState()
    val engineEntries = remember(engines, preferences.engineSelection.get()) {
        buildMap {
            put(TranslationEngineSelection.Automatic, "")
            engines.forEach { engine ->
                val name = "${engine.engineName} (${engine.providerName})"
                put(
                    TranslationEngineSelection.Explicit(engine.id),
                    when (val availability = engine.buildAvailability) {
                        TranslationEngineBuildAvailability.Included -> name
                        is TranslationEngineBuildAvailability.NotIncluded -> "$name — ${availability.reason}"
                    },
                )
            }
            val saved = preferences.engineSelection.get()
            if (saved is TranslationEngineSelection.Explicit && saved !in keys) {
                put(saved, "")
            }
        }
    }
    val documentation = remember(engines) {
        engines.mapNotNull { engine ->
            engine.documentationUrl?.let { url -> engine to url }
        }
    }

    return buildList {
        add(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.translation_settings_configuration),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = preferences.engineSelection,
                        entries = engineEntries.mapValues { (selection, label) ->
                            when (selection) {
                                TranslationEngineSelection.Automatic ->
                                    stringResource(MR.strings.translation_settings_engine_automatic)
                                is TranslationEngineSelection.Explicit -> label.ifEmpty {
                                    stringResource(
                                        MR.strings.translation_settings_engine_unknown,
                                        selection.engine.value,
                                    )
                                }
                            }
                        },
                        title = stringResource(MR.strings.translation_settings_engine),
                        subtitleProvider = { selection, _ ->
                            engineSelectionSummary(selection, engines)
                        },
                        entryEnabledProvider = { selection ->
                            when (selection) {
                                TranslationEngineSelection.Automatic -> true
                                is TranslationEngineSelection.Explicit ->
                                    engines
                                        .firstOrNull { it.id == selection.engine }
                                        ?.buildAvailability is TranslationEngineBuildAvailability.Included
                            }
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.translation_settings_target),
                        subtitle = when (val targetSelection = target) {
                            TranslationTargetLanguageSelection.Default -> defaultTargetLabel()
                            is TranslationTargetLanguageSelection.Explicit -> targetSelection.language.displayName()
                        },
                        isProfileSpecific = true,
                        onClick = onChooseTarget,
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(MR.strings.translation_settings_system_notice),
                    ),
                ),
            ),
        )
        add(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.translation_settings_try_it),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.translation_settings_test),
                        subtitle = stringResource(MR.strings.translation_settings_test_summary),
                        onClick = onTest,
                    ),
                ),
            ),
        )
        if (documentation.isNotEmpty()) {
            add(
                Preference.PreferenceGroup(
                    title = stringResource(MR.strings.label_help),
                    preferenceItems = documentation.map { (engine, url) ->
                        Preference.PreferenceItem.TextPreference(
                            title = stringResource(
                                MR.strings.translation_settings_provider_documentation,
                                engine.providerName,
                            ),
                            subtitle = engine.engineName,
                            onClick = { onOpenDocumentation(url) },
                        )
                    },
                ),
            )
        }
    }
}

@Composable
private fun engineSelectionSummary(
    selection: TranslationEngineSelection,
    engines: List<KnownTranslationEngine>,
): String {
    if (selection == TranslationEngineSelection.Automatic) {
        return stringResource(MR.strings.translation_settings_engine_automatic_summary)
    }
    val explicit = selection as TranslationEngineSelection.Explicit
    val engine = engines.firstOrNull { it.id == explicit.engine }
        ?: return stringResource(
            MR.strings.translation_settings_engine_unknown,
            explicit.engine.value,
        )
    return when (val availability = engine.buildAvailability) {
        TranslationEngineBuildAvailability.Included -> stringResource(
            MR.strings.translation_settings_engine_available_on_test,
            engine.providerName,
        )
        is TranslationEngineBuildAvailability.NotIncluded -> availability.reason
    }
}

@Composable
private fun defaultTargetLabel(): String {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
        ?: LocaleListCompat.getAdjustedDefault().get(0)
        ?: Locale.getDefault()
    return stringResource(
        MR.strings.translation_settings_target_default,
        locale.getDisplayName(Locale.getDefault()).ifBlank { locale.toLanguageTag() },
    )
}

private sealed interface TranslationLanguageDialog {
    data object DefaultTarget : TranslationLanguageDialog

    data object RequestSource : TranslationLanguageDialog

    data object RequestTarget : TranslationLanguageDialog

    data class Pair(
        val source: TranslationLanguageTag,
        val target: TranslationLanguageTag,
    ) : TranslationLanguageDialog

    data class PairPicker(
        val pair: Pair,
        val field: TranslationLanguageField,
    ) : TranslationLanguageDialog
}

private enum class TranslationLanguageField {
    Source,
    Target,
}
