package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import eu.kanade.presentation.more.settings.screen.translation.language.defaultTranslationTargetLabel
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import eu.kanade.presentation.more.settings.widget.highlightBackground
import kotlinx.coroutines.delay
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.displayName
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationSessionPanel
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
    engines: List<KnownTranslationEngine>,
    defaultEngine: TranslationEngineId,
    defaultTarget: TranslationTargetLanguageSelection,
    deviceAvailability: TranslationDeviceAvailability?,
    controller: TranslationSessionController,
    searchHighlightKey: String?,
    onSearchHighlightConsumed: (String) -> Unit,
    onBack: (() -> Unit)?,
    onTextChange: (String) -> Unit,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwapLanguages: () -> Unit,
    onChooseEngine: () -> Unit,
    onChooseDefaultEngine: () -> Unit,
    onChooseDefaultTarget: () -> Unit,
    onUseEngineAsDefault: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    val playgroundTitle = stringResource(MR.strings.translation_settings_playground)
    val engineTitle = stringResource(MR.strings.translation_settings_engine)
    val targetTitle = stringResource(MR.strings.translation_settings_target)
    val unavailableMessage = deviceAvailabilityMessage(deviceAvailability)
    val listState = rememberLazyListState()
    val highlightedItemIndex = when (searchHighlightKey) {
        playgroundTitle -> PLAYGROUND_ITEM_INDEX
        engineTitle,
        targetTitle,
        -> DEFAULTS_ITEM_INDEX
        else -> null
    }
    LaunchedEffect(searchHighlightKey, highlightedItemIndex) {
        val key = searchHighlightKey ?: return@LaunchedEffect
        if (highlightedItemIndex != null) {
            delay(SEARCH_HIGHLIGHT_SCROLL_DELAY)
            listState.animateScrollToItem(highlightedItemIndex)
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
                SectionTitle(stringResource(MR.strings.translation_settings_playground))
            }
            item {
                TranslationPlayground(
                    state = playground,
                    engines = engines,
                    defaultEngine = defaultEngine,
                    controller = controller,
                    onTextChange = onTextChange,
                    onChooseSource = onChooseSource,
                    onChooseTarget = onChooseTarget,
                    onSwapLanguages = onSwapLanguages,
                    onChooseEngine = onChooseEngine,
                    onUseEngineAsDefault = onUseEngineAsDefault,
                    onExternalAction = onExternalAction,
                    highlighted = searchHighlightKey == playgroundTitle,
                )
            }
            item {
                SectionTitle(stringResource(MR.strings.translation_settings_defaults))
            }
            item {
                TranslationDefaults(
                    engines = engines,
                    defaultEngine = defaultEngine,
                    defaultTarget = defaultTarget,
                    onChooseEngine = onChooseDefaultEngine,
                    onChooseTarget = onChooseDefaultTarget,
                    highlightedEngine = searchHighlightKey == engineTitle,
                    highlightedTarget = searchHighlightKey == targetTitle,
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(MR.strings.translation_settings_system_notice_explicit),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (unavailableMessage != null) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = MaterialTheme.padding.large),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = unavailableMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun deviceAvailabilityMessage(availability: TranslationDeviceAvailability?): String? {
    return when (availability) {
        null,
        TranslationDeviceAvailability.Available,
        -> null
        is TranslationDeviceAvailability.SelectedEngineMissing,
        is TranslationDeviceAvailability.SelectedEngineUnavailable,
        -> stringResource(MR.strings.translation_selected_engine_unavailable)
        is TranslationDeviceAvailability.UnsupportedOs ->
            stringResource(MR.strings.translation_unsupported_os, availability.minimumApi)
        TranslationDeviceAvailability.TranslationServiceMissing ->
            stringResource(MR.strings.translation_service_missing)
        is TranslationDeviceAvailability.ProviderFailure ->
            stringResource(
                MR.strings.translation_engine_unavailable,
                availability.engine.value,
                availability.reason,
            )
    }
}

@Composable
private fun TranslationPlayground(
    state: TranslationPlaygroundState,
    engines: List<KnownTranslationEngine>,
    defaultEngine: TranslationEngineId,
    controller: TranslationSessionController,
    onTextChange: (String) -> Unit,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwapLanguages: () -> Unit,
    onChooseEngine: () -> Unit,
    onUseEngineAsDefault: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    highlighted: Boolean,
) {
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
            Column {
                Text(
                    text = stringResource(MR.strings.translation_settings_try_translation),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(MR.strings.translation_settings_try_translation_summary),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
                IconButton(onClick = onSwapLanguages) {
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
                value = translationEngineLabel(state.engine, engines),
                leadingIcon = {
                    Icon(Icons.Outlined.Settings, contentDescription = null)
                },
                onClick = onChooseEngine,
                modifier = Modifier.testTag(TRANSLATION_ENGINE_TAG),
            )
            if (state.engine != defaultEngine) {
                TextButton(
                    onClick = onUseEngineAsDefault,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(MR.strings.translation_settings_use_engine_as_default))
                }
            }
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
                minLines = 3,
                maxLines = 8,
            )
            TranslationSessionPanel(
                controller = controller,
                onExternalAction = onExternalAction,
                modifier = Modifier.testTag(TRANSLATION_OUTPUT_TAG),
            )
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

@Composable
private fun TranslationDefaults(
    engines: List<KnownTranslationEngine>,
    defaultEngine: TranslationEngineId,
    defaultTarget: TranslationTargetLanguageSelection,
    onChooseEngine: () -> Unit,
    onChooseTarget: () -> Unit,
    highlightedEngine: Boolean,
    highlightedTarget: Boolean,
) {
    OutlinedCard(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(MR.strings.translation_settings_engine)) },
            supportingContent = { Text(translationEngineLabel(defaultEngine, engines)) },
            leadingContent = {
                Icon(Icons.Outlined.Settings, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            },
            modifier = Modifier
                .highlightBackground(highlightedEngine)
                .clickable(onClick = onChooseEngine),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(MR.strings.translation_settings_target)) },
            supportingContent = {
                Text(
                    when (defaultTarget) {
                        TranslationTargetLanguageSelection.Default -> defaultTranslationTargetLabel()
                        is TranslationTargetLanguageSelection.Explicit ->
                            defaultTarget.language.displayName()
                    },
                )
            },
            leadingContent = {
                Icon(Icons.Outlined.Language, contentDescription = null)
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            },
            modifier = Modifier
                .highlightBackground(highlightedTarget)
                .clickable(onClick = onChooseTarget),
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(
            start = MaterialTheme.padding.large,
            top = MaterialTheme.padding.medium,
            end = MaterialTheme.padding.large,
        ),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

internal const val TRANSLATION_PLAYGROUND_TAG = "translation_playground"
internal const val TRANSLATION_SOURCE_TAG = "translation_source"
internal const val TRANSLATION_TARGET_TAG = "translation_target"
internal const val TRANSLATION_ENGINE_TAG = "translation_engine"
internal const val TRANSLATION_INPUT_TAG = "translation_input"
internal const val TRANSLATION_OUTPUT_TAG = "translation_output"

private const val PLAYGROUND_ITEM_INDEX = 1
private const val DEFAULTS_ITEM_INDEX = 3
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds
