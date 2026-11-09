package eu.kanade.tachiyomi.ui.translator.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Settings
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.layout.FramedContent
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.profiles.ui.switcher.ActiveProfileButton
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineSelection
import app.katari.translation.api.request.TranslationSourceLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.ui.picker.engine.TranslationEnginePickerList
import app.katari.translation.ui.picker.engine.TranslationEngineSelectorRow
import app.katari.translation.ui.picker.language.TranslationLanguageDefaultOption
import app.katari.translation.ui.picker.language.TranslationLanguagePairSelector
import app.katari.translation.ui.picker.language.TranslationLanguageRole
import app.katari.translation.ui.picker.language.TranslationLanguageSupportPicker
import app.katari.translation.ui.picker.language.displayName
import app.katari.translation.ui.picker.language.translationDefaultTargetOption
import app.katari.translation.ui.presentation.TranslationPickerSheet
import app.katari.translation.ui.presentation.TranslationResultSpeechSide
import app.katari.translation.ui.presentation.TranslationResultSpeechTarget
import app.katari.translation.ui.presentation.TranslationSessionExternalAction
import app.katari.translation.ui.presentation.TranslationWorkbench
import app.katari.translation.ui.session.TranslationLanguageSupportState
import app.katari.translation.ui.session.displayedSessionResult
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.ui.translator.session.TranslatorPicker
import eu.kanade.tachiyomi.ui.translator.session.TranslatorState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslatorScreenContent(
    state: TranslatorState,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    syncAction: AppBar.Action?,
    onTextChange: (String) -> Unit,
    onClearText: () -> Unit,
    onShowPicker: (TranslatorPicker) -> Unit,
    onDismissPicker: () -> Unit,
    onSelectAutomaticSource: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectTarget: (LanguageTag) -> Unit,
    onSelectDefaultTarget: () -> Unit,
    onSelectEngine: (TranslationEngineId) -> Unit,
    onSwap: () -> Unit,
    onRetry: () -> Unit,
    onExecute: () -> Unit,
    onRetryLanguageSupport: () -> Unit,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val textClipboard = graph.textClipboard
    val textSharing = graph.textSharing
    val clipboardLabel = stringResource(MR.strings.translator_output_label)
    val selectedEngine = state.engines.firstOrNull { it.engine.id == state.activeEngine }
    val successful = state.session.displayedSessionResult()
    val sourceSpeechTarget = successful
        ?.takeIf { it.input.request.text == state.text }
        ?.let {
            TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Source,
                text = it.input.request.text,
                language = it.result.sourceLanguage,
            )
        }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.translator_title),
                navigateUp = onNavigateUp,
                actions = {
                    AppBarActions(
                        listOfNotNull(
                            syncAction,
                            AppBar.Action(
                                title = stringResource(MR.strings.translator_open_settings),
                                icon = MaterialSymbols.Rounded.Settings,
                                onClick = onOpenSettings,
                            ),
                        ),
                    )
                    ActiveProfileButton()
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
    ) { contentPadding ->
        FramedContent(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s)
                    .fillMaxWidth()
                    .widthIn(max = KatariTheme.sizes.readableWidth),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TranslationLanguagePairSelector(
                    source = sourceLanguageLabel(state),
                    target = targetLanguageLabel(state),
                    // The swap control stays enabled; TranslatorViewModel answers every click either
                    // by exchanging the languages or with a SwapUnavailable event.
                    canSwap = true,
                    onChooseSource = { onShowPicker(TranslatorPicker.SourceLanguage) },
                    onChooseTarget = { onShowPicker(TranslatorPicker.TargetLanguage) },
                    onSwap = onSwap,
                )
                TranslationEngineSelectorRow(
                    engineName = when {
                        !state.engineSelectionResolved ->
                            stringResource(MR.strings.translation_engine_status_checking)
                        selectedEngine != null -> selectedEngine.engine.engineName
                        else -> stringResource(MR.strings.translation_choose_engine)
                    },
                    onClick = { onShowPicker(TranslatorPicker.Engine) },
                )
                TranslationWorkbench(
                    text = state.text,
                    session = state.session,
                    sourceSpeechTarget = sourceSpeechTarget,
                    speechState = state.speech,
                    onTextChange = onTextChange,
                    onClear = onClearText,
                    onSpeechToggle = onSpeechToggle,
                    onCopy = { text -> textClipboard.copy(clipboardLabel, text) },
                    onShare = { text: String -> textSharing.share(context, text) }.takeIf { textSharing.isAvailable },
                    onExecute = onExecute,
                    onRetry = onRetry,
                    onSelectSource = onSelectSource,
                    onSelectEngine = { selection ->
                        (selection as? TranslationEngineSelection.Explicit)?.engine?.let(onSelectEngine)
                    },
                    onExternalAction = onExternalAction,
                )
            }
        }
    }

    state.picker?.let { picker ->
        TranslatorPicker(
            picker = picker,
            state = state,
            onDismiss = onDismissPicker,
            onOpenSettings = onOpenSettings,
            onSelectAutomaticSource = onSelectAutomaticSource,
            onSelectSource = onSelectSource,
            onSelectTarget = onSelectTarget,
            onSelectDefaultTarget = onSelectDefaultTarget,
            onSelectEngine = onSelectEngine,
            onRetryLanguageSupport = onRetryLanguageSupport,
        )
    }
}

@Composable
private fun TranslatorPicker(
    picker: TranslatorPicker,
    state: TranslatorState,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectAutomaticSource: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectTarget: (LanguageTag) -> Unit,
    onSelectDefaultTarget: () -> Unit,
    onSelectEngine: (TranslationEngineId) -> Unit,
    onRetryLanguageSupport: () -> Unit,
) {
    TranslationPickerSheet(
        title = stringResource(
            when (picker) {
                TranslatorPicker.SourceLanguage -> MR.strings.translation_choose_source_language
                TranslatorPicker.TargetLanguage -> MR.strings.translation_choose_target_language
                TranslatorPicker.Engine -> MR.strings.translation_choose_engine
            },
        ),
        onDismiss = onDismiss,
    ) {
        when (picker) {
            TranslatorPicker.Engine -> TranslationEnginePickerList(
                engines = state.engines,
                selected = state.activeEngine,
                onSelect = onSelectEngine,
                selectableOnly = true,
                modifier = Modifier.weight(1f, fill = false),
                footerLabel = stringResource(MR.strings.translator_open_settings),
                onFooterClick = {
                    onDismiss()
                    onOpenSettings()
                },
            )
            TranslatorPicker.SourceLanguage,
            TranslatorPicker.TargetLanguage,
            -> {
                val sourcePicker = picker == TranslatorPicker.SourceLanguage
                TranslationLanguageSupportPicker(
                    state = state.languageSupport,
                    engine = state.activeEngine,
                    role = if (sourcePicker) TranslationLanguageRole.Source else TranslationLanguageRole.Target,
                    counterpart = if (sourcePicker) effectiveTargetLanguage(state) else effectiveSourceLanguage(state),
                    selected = if (sourcePicker) state.explicitSourceLanguage else state.explicitTargetLanguage,
                    onSelect = if (sourcePicker) onSelectSource else onSelectTarget,
                    onRetry = onRetryLanguageSupport,
                    engineName = state.engines.firstOrNull { it.engine.id == state.activeEngine }?.engine?.engineName,
                    modifier = Modifier.weight(1f, fill = false),
                    defaultOption = if (sourcePicker) {
                        TranslationLanguageDefaultOption(
                            label = stringResource(MR.strings.translator_detect_language),
                            supporting = stringResource(MR.strings.translator_detect_language_summary),
                            selected = state.sourceLanguage == TranslationSourceLanguageSelection.Automatic,
                        )
                    } else {
                        state.defaultTarget?.let { target ->
                            translationDefaultTargetOption(
                                target = target,
                                selected = state.targetLanguage == TranslationTargetLanguageSelection.Default,
                            )
                        }
                    },
                    onSelectDefault = if (sourcePicker) onSelectAutomaticSource else onSelectDefaultTarget,
                    recentLanguages = state.recentLanguages,
                )
            }
        }
    }
}

@Composable
private fun sourceLanguageLabel(state: TranslatorState): String {
    return when (val source = state.sourceLanguage) {
        is TranslationSourceLanguageSelection.Explicit -> source.language.displayName()
        TranslationSourceLanguageSelection.Automatic ->
            state.session
                .displayedSessionResult()
                ?.result
                ?.sourceLanguage
                ?.let { stringResource(MR.strings.translator_detected_language, it.displayName()) }
                ?: stringResource(MR.strings.translator_detect_language)
    }
}

@Composable
private fun targetLanguageLabel(state: TranslatorState): String {
    return effectiveTargetLanguage(state)?.displayName()
        ?: stringResource(MR.strings.translation_choose_target_language)
}

private fun effectiveSourceLanguage(state: TranslatorState): LanguageTag? =
    state.explicitSourceLanguage ?: state.session.displayedSessionResult()?.result?.sourceLanguage

private fun effectiveTargetLanguage(state: TranslatorState): LanguageTag? =
    when (val target = state.targetLanguage) {
        is TranslationTargetLanguageSelection.Explicit -> target.language
        TranslationTargetLanguageSelection.Default ->
            state.session.displayedSessionResult()?.result?.targetLanguage ?: state.defaultTarget?.language
    }
