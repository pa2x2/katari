package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.screen.translation.TranslationPlaygroundState
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.surface.pulsingHighlight
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.ui.picker.engine.TranslationEngineSelectorRow
import mihon.translation.ui.picker.language.TranslationLanguagePairSelector
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.supportsPair
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationWorkbench
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionController
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Tries a real translation with the profile's draft languages and engine, and offers the engine's setup. */
@Composable
internal fun TranslationPlaygroundCard(
    state: TranslationPlaygroundState,
    engines: List<TranslationEngineState>,
    languageSupport: TranslationLanguageSupportState,
    controller: TranslationSessionController,
    onTextChange: (String) -> Unit,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwapLanguages: () -> Unit,
    onChooseEngine: () -> Unit,
    canOpenSetup: Boolean,
    onOpenSetup: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    highlighted: Boolean,
) {
    val selectedProviderName = engines
        .firstOrNull { it.engine.id == state.engine }
        ?.engine
        ?.providerName
    val availableLanguageSupport = (languageSupport as? TranslationLanguageSupportState.Available)
        ?.takeIf { it.engine == state.engine }
        ?.support
    val hasSupportedPair = availableLanguageSupport?.supportsPair(
        state.sourceLanguage,
        state.targetLanguage,
    ) == true
    val canSwapLanguages = availableLanguageSupport?.supportsPair(
        state.targetLanguage,
        state.sourceLanguage,
    ) == true
    val session by controller.state.collectAsState()

    ControlRow(modifier = Modifier.pulsingHighlight(Unit.takeIf { highlighted })) {
        TranslationLanguagePairSelector(
            source = state.sourceLanguage.displayName(),
            target = state.targetLanguage.displayName(),
            canSwap = canSwapLanguages,
            onChooseSource = onChooseSource,
            onChooseTarget = onChooseTarget,
            onSwap = onSwapLanguages,
        )
        TranslationEngineSelectorRow(
            engineName = when {
                !state.engineSelectionResolved ->
                    stringResource(MR.strings.translation_engine_status_checking)
                state.engine == null ->
                    stringResource(MR.strings.translation_choose_engine)
                else -> translationEngineLabel(state.engine, engines.map { it.engine })
            },
            onClick = onChooseEngine,
        )
        TranslationWorkbench(
            text = state.text,
            session = session,
            sourceSpeechTarget = null,
            speechState = TranslationResultSpeechState(),
            onTextChange = onTextChange,
            onClear = { onTextChange("") },
            onSpeechToggle = {},
            onExecute = controller::execute,
            onRetry = controller::retry,
            onSelectSource = controller::selectSourceLanguage,
            onSelectEngine = controller::selectEngine,
            onExternalAction = onExternalAction,
            modifier = Modifier.fillMaxWidth(),
            inputPlaceholder = stringResource(MR.strings.translation_settings_test_input),
        )
        if (
            languageSupport is TranslationLanguageSupportState.Available &&
            !hasSupportedPair
        ) {
            Text(
                text = stringResource(MR.strings.translation_language_pair_required),
                role = TextRole.Body,
                tone = ContentTone.Error,
            )
        }
        if (canOpenSetup && selectedProviderName != null) {
            Button(
                label = stringResource(
                    MR.strings.translation_settings_open_provider_settings,
                    selectedProviderName,
                ),
                onClick = onOpenSetup,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = ButtonStyle.Text,
                icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
            )
        }
    }
}
