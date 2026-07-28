package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.screen.translation.TranslationPlaygroundState
import eu.kanade.presentation.more.settings.screen.translation.engine.translationEngineLabel
import eu.kanade.presentation.more.settings.screen.translation.language.defaultTranslationTargetLabel
import eu.kanade.presentation.more.settings.screen.translation.language.displayName
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationSessionPanel
import mihon.translation.ui.session.TranslationSessionController
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationSettingsContent(
    playground: TranslationPlaygroundState,
    engines: List<KnownTranslationEngine>,
    defaultEngine: TranslationEngineId,
    defaultTarget: TranslationTargetLanguageSelection,
    controller: TranslationSessionController,
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
        }
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
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .fillMaxWidth()
            .testTag(TRANSLATION_PLAYGROUND_TAG),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.large),
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
            modifier = Modifier.clickable(onClick = onChooseEngine),
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
            modifier = Modifier.clickable(onClick = onChooseTarget),
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
