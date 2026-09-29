package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.screen.translation.TranslationPlaygroundState
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import eu.kanade.presentation.more.settings.screen.translation.series.SeriesTranslationLanguagesEntry
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import eu.kanade.presentation.more.settings.widget.draft.SettingsDraftSaveBar
import eu.kanade.presentation.more.settings.widget.draft.rememberSettingsDraftLeaveGuard
import kotlinx.coroutines.delay
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.ui.picker.engine.TranslationEngineSelectorRow
import mihon.translation.ui.picker.language.TranslationLanguagePairSelector
import mihon.translation.ui.picker.language.TranslationLanguagePairSelectorStyle
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.supportsPair
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationWorkbench
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionController
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.pulsingHighlightBackground
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TranslationSettingsContent(
    playground: TranslationPlaygroundState,
    engines: List<TranslationEngineState>,
    languageSupport: TranslationLanguageSupportState,
    controller: TranslationSessionController,
    searchHighlightKey: String?,
    onSearchHighlightConsumed: (String) -> Unit,
    onBack: (() -> Unit)?,
    onTextChange: (String) -> Unit,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwapLanguages: () -> Unit,
    onChooseEngine: () -> Unit,
    canOpenSetup: Boolean,
    onOpenSetup: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    seriesLanguageCount: Int?,
    onOpenSeriesLanguages: () -> Unit,
) {
    val playgroundTitle = stringResource(MR.strings.translation_settings_playground)
    val engineTitle = stringResource(MR.strings.translation_settings_engine)
    val targetTitle = stringResource(MR.strings.translation_settings_target)
    val listState = rememberLazyListState()
    val highlightPlayground = searchHighlightKey == playgroundTitle ||
        searchHighlightKey == engineTitle ||
        searchHighlightKey == targetTitle
    LaunchedEffect(searchHighlightKey, highlightPlayground) {
        val key = searchHighlightKey ?: return@LaunchedEffect
        if (highlightPlayground) {
            delay(SEARCH_HIGHLIGHT_SCROLL_DELAY)
            listState.animateScrollToItem(PLAYGROUND_ITEM_INDEX)
        }
        onSearchHighlightConsumed(key)
    }
    val saveEnabled = playground.hasUnsavedProfileChanges &&
        (languageSupport as? TranslationLanguageSupportState.Available)
            ?.takeIf { it.engine == playground.engine }
            ?.support
            ?.supportsPair(playground.sourceLanguage, playground.targetLanguage) == true
    val navigateUp = rememberSettingsDraftLeaveGuard(
        hasUnsavedChanges = playground.hasUnsavedProfileChanges,
        saveEnabled = saveEnabled,
        onSave = onSave,
        onDiscard = onDiscard,
        onLeave = onBack,
    )

    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.translation_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack?.let { navigateUp },
                scrollBehavior = it,
            )
        },
        bottomBar = {
            SettingsDraftSaveBar(
                visible = playground.hasUnsavedProfileChanges,
                saveEnabled = saveEnabled,
                onDiscard = onDiscard,
                onSave = onSave,
            )
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            item {
                TranslationPlayground(
                    state = playground,
                    engines = engines,
                    languageSupport = languageSupport,
                    controller = controller,
                    onTextChange = onTextChange,
                    onChooseSource = onChooseSource,
                    onChooseTarget = onChooseTarget,
                    onSwapLanguages = onSwapLanguages,
                    onChooseEngine = onChooseEngine,
                    canOpenSetup = canOpenSetup,
                    onOpenSetup = onOpenSetup,
                    onExternalAction = onExternalAction,
                    highlighted = highlightPlayground,
                )
            }
            item {
                SeriesTranslationLanguagesEntry(count = seriesLanguageCount, onClick = onOpenSeriesLanguages)
            }
        }
    }
}

@Composable
private fun TranslationPlayground(
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

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .pulsingHighlightBackground(Unit.takeIf { highlighted })
                .padding(MaterialTheme.padding.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            TranslationLanguagePairSelector(
                source = state.sourceLanguage.displayName(),
                target = state.targetLanguage.displayName(),
                canSwap = canSwapLanguages,
                onChooseSource = onChooseSource,
                onChooseTarget = onChooseTarget,
                onSwap = onSwapLanguages,
                style = TranslationLanguagePairSelectorStyle.Bar,
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
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (canOpenSetup && selectedProviderName != null) {
                TextButton(
                    onClick = onOpenSetup,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(
                            MR.strings.translation_settings_open_provider_settings,
                            selectedProviderName,
                        ),
                        modifier = Modifier.padding(start = MaterialTheme.padding.small),
                    )
                }
            }
        }
    }
}

private const val PLAYGROUND_ITEM_INDEX = 0
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds
