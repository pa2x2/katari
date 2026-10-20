package eu.kanade.tachiyomi.ui.translator.presentation

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import eu.kanade.tachiyomi.ui.translator.session.TranslatorPicker
import eu.kanade.tachiyomi.ui.translator.session.TranslatorState
import kotlinx.coroutines.launch
import mihon.feature.profiles.ui.switcher.ActiveProfileButton
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.engine.TranslationEnginePickerList
import mihon.translation.ui.picker.engine.TranslationEngineSelectorRow
import mihon.translation.ui.picker.language.TranslationLanguageDefaultOption
import mihon.translation.ui.picker.language.TranslationLanguagePairSelector
import mihon.translation.ui.picker.language.TranslationLanguageRole
import mihon.translation.ui.picker.language.TranslationLanguageSupportPicker
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.translationDefaultTargetOption
import mihon.translation.ui.presentation.TranslationPickerSheet
import mihon.translation.ui.presentation.TranslationResultSpeechSide
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationWorkbench
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.displayedSessionResult
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslatorScreenContent(
    state: TranslatorState,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: (() -> Unit)?,
    onOpenSettings: () -> Unit,
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
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipboardLabel = stringResource(MR.strings.translator_output_label)
    val shareLabel = stringResource(MR.strings.action_share)
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
                        listOf(
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
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
                onCopy = { text ->
                    scope.launch {
                        clipboard.setClipEntry(ClipData.newPlainText(clipboardLabel, text).toClipEntry())
                    }
                },
                onShare = { text ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, shareLabel))
                },
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
