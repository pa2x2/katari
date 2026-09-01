package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.screen.translation.TranslationPlaygroundState
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
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
import tachiyomi.i18n.MR
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
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
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

    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.translation_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack,
                scrollBehavior = it,
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
                    onSave = onSave,
                    onExternalAction = onExternalAction,
                    highlighted = highlightPlayground,
                )
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
    onSave: () -> Unit,
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
            .fillMaxWidth()
            .testTag(TRANSLATION_PLAYGROUND_TAG),
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
                sourceModifier = Modifier.testTag(TRANSLATION_SOURCE_TAG),
                targetModifier = Modifier.testTag(TRANSLATION_TARGET_TAG),
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
                modifier = Modifier.testTag(TRANSLATION_ENGINE_TAG),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRANSLATION_INPUT_TAG),
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
            Button(
                onClick = onSave,
                enabled = state.hasUnsavedProfileChanges && hasSupportedPair,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRANSLATION_SAVE_TAG),
            ) {
                Text(stringResource(MR.strings.action_save))
            }
            if (canOpenSetup && selectedProviderName != null) {
                TextButton(
                    onClick = onOpenSetup,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(TRANSLATION_SYSTEM_SETUP_TAG),
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

internal const val TRANSLATION_PLAYGROUND_TAG = "translation_playground"
internal const val TRANSLATION_SOURCE_TAG = "translation_source"
internal const val TRANSLATION_TARGET_TAG = "translation_target"
internal const val TRANSLATION_ENGINE_TAG = "translation_engine"
internal const val TRANSLATION_SYSTEM_SETUP_TAG = "translation_system_setup"
internal const val TRANSLATION_INPUT_TAG = "translation_input"
internal const val TRANSLATION_SAVE_TAG = "translation_save"

private const val PLAYGROUND_ITEM_INDEX = 0
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds
