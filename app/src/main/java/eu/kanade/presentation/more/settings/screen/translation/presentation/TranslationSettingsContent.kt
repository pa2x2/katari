package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.screen.translation.TranslationPlaygroundState
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import eu.kanade.presentation.more.settings.widget.highlightBackground
import kotlinx.coroutines.delay
import mihon.translation.api.TranslationEngineState
import mihon.translation.ui.picker.displayName
import mihon.translation.ui.picker.supportsPair
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationSessionPanel
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionController
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
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

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .fillMaxWidth()
            .testTag(TRANSLATION_PLAYGROUND_TAG),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .highlightBackground(highlighted)
                .padding(MaterialTheme.padding.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaygroundSelector(
                    label = stringResource(MR.strings.translation_settings_from),
                    value = state.sourceLanguage.displayName(),
                    onClick = onChooseSource,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TRANSLATION_SOURCE_TAG),
                )
                IconButton(
                    onClick = onSwapLanguages,
                    enabled = canSwapLanguages,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = stringResource(
                            MR.strings.translation_settings_swap_languages,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PlaygroundSelector(
                    label = stringResource(MR.strings.translation_settings_to),
                    value = state.targetLanguage.displayName(),
                    onClick = onChooseTarget,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TRANSLATION_TARGET_TAG),
                )
            }
            PlaygroundSelector(
                label = stringResource(MR.strings.translation_settings_engine),
                value = when {
                    !state.engineSelectionResolved ->
                        stringResource(MR.strings.translation_engine_status_checking)
                    state.engine == null ->
                        stringResource(MR.strings.translation_choose_engine)
                    else -> translationEngineLabel(state.engine, engines.map { it.engine })
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Settings, contentDescription = null)
                },
                onClick = onChooseEngine,
                modifier = Modifier.testTag(TRANSLATION_ENGINE_TAG),
            )
            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRANSLATION_INPUT_TAG),
                label = { Text(stringResource(MR.strings.translation_settings_test_input)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Translate, contentDescription = null)
                },
                minLines = 1,
                maxLines = 4,
            )
            TranslationSessionPanel(
                controller = controller,
                onExternalAction = onExternalAction,
                modifier = Modifier.testTag(TRANSLATION_OUTPUT_TAG),
                showCopy = false,
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
                        imageVector = Icons.Outlined.OpenInNew,
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

@Composable
private fun PlaygroundSelector(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
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
internal const val TRANSLATION_OUTPUT_TAG = "translation_output"
internal const val TRANSLATION_SAVE_TAG = "translation_save"

private const val PLAYGROUND_ITEM_INDEX = 0
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds
