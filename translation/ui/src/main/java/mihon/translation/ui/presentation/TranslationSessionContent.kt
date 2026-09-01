package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationRejectionReason
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.session.TranslationSessionFailure
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedSessionResult
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

@Composable
internal fun TranslationSessionContent(
    state: TranslationSessionState.Active,
    expanded: Boolean,
    showHeader: Boolean,
    showExpand: Boolean,
    showLanguageChange: Boolean,
    showEngineChange: Boolean,
    showCopy: Boolean,
    useExternalEnginePicker: Boolean,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showProgress: Boolean = true,
    showResultLanguage: Boolean = true,
    flushContent: Boolean = false,
) {
    val inProgress = state is TranslationSessionState.Settling ||
        state is TranslationSessionState.Preparing ||
        state is TranslationSessionState.Translating
    val displayedSessionResult = state.displayedSessionResult()
    val displayedResult = displayedSessionResult?.result

    val compactResult = compact && displayedResult != null
    Column(
        modifier = modifier
            .then(
                if (compactResult) {
                    Modifier.width(IntrinsicSize.Max)
                } else {
                    Modifier.fillMaxWidth()
                },
            )
            .testTag(TRANSLATION_SESSION_CONTENT_TAG),
    ) {
        if (showProgress && inProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRANSLATION_SESSION_PROGRESS_TAG),
            )
        }
        if (showHeader && !compactResult) {
            val documentationUrl = state.contextualDocumentationUrl().takeIf { compact }
            TranslationSessionHeader(
                title = stringResource(MR.strings.translation_title),
                onDismiss = onDismiss,
                compact = compact,
                onExpand = onExpand.takeIf {
                    compact && showExpand && state.hasExpandedCompactContent()
                },
                onDocumentation = documentationUrl?.let { url ->
                    {
                        onExternalAction(TranslationSessionExternalAction.OpenDocumentation(url))
                    }
                },
            )
            HorizontalDivider()
        }
        Column(
            modifier = if (flushContent) {
                Modifier
            } else {
                Modifier.padding(
                    horizontal = if (compact) 16.dp else 20.dp,
                    vertical = if (compact) 12.dp else 20.dp,
                )
            },
            verticalArrangement = Arrangement.spacedBy(
                when {
                    compactResult -> 4.dp
                    compact -> 8.dp
                    else -> 12.dp
                },
            ),
        ) {
            if (displayedResult != null) {
                SuccessContent(
                    result = displayedResult,
                    sourceText = displayedSessionResult.input.request.text,
                    expanded = expanded,
                    compact = compact,
                    showExpand = showExpand,
                    showLanguageChange = showLanguageChange,
                    showEngineChange = showEngineChange,
                    showCopy = showCopy,
                    onDismiss = if (compactResult) onDismiss else null,
                    onCopy = onCopy,
                    onExpand = onExpand,
                    onExternalAction = onExternalAction,
                    speechState = speechState,
                    onSpeechToggle = onSpeechToggle,
                    showResultLanguage = showResultLanguage,
                )
            }
            when (state) {
                is TranslationSessionState.Settling,
                is TranslationSessionState.Preparing,
                is TranslationSessionState.Translating,
                is TranslationSessionState.Success,
                -> Unit
                is TranslationSessionState.Ready -> ReadyContent(state, onExecute, compact)
                is TranslationSessionState.PreparationRequired -> PreparationContent(
                    preparation = state.preparation,
                    onRetry = onRetry,
                    onSelectSource = onSelectSource,
                    onSelectEngine = onSelectEngine,
                    useExternalEnginePicker = useExternalEnginePicker,
                    onExternalAction = onExternalAction,
                    compact = compact,
                )
                is TranslationSessionState.ProviderSurfaceOpened -> SessionMessage(
                    text = stringResource(
                        MR.strings.translation_provider_surface_opened,
                        state.presentation.providerName,
                    ),
                    compact = compact,
                )
                is TranslationSessionState.Failed -> FailedContent(
                    state = state,
                    onRetry = onRetry,
                    onExternalAction = onExternalAction,
                    compact = compact,
                )
            }
        }
    }
}

@Composable
internal fun TranslationSessionHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onExpand: (() -> Unit)? = null,
    onDocumentation: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) 16.dp else 20.dp,
                end = if (compact) 4.dp else 8.dp,
                top = if (compact) 4.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        onDocumentation?.let {
            TranslationCompactIconButton(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = stringResource(MR.strings.action_learn_more),
                onClick = it,
            )
        }
        onExpand?.let {
            TranslationCompactIconButton(
                icon = Icons.Outlined.OpenInFull,
                contentDescription = stringResource(MR.strings.action_expand),
                onClick = it,
            )
        }
        if (compact) {
            TranslationCompactIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.action_close),
                onClick = onDismiss,
            )
        } else {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(MR.strings.action_close),
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: TranslationSessionState.Ready,
    onExecute: () -> Unit,
    compact: Boolean,
) {
    Text(
        text = state.preparation.presentation.engineName,
        style = MaterialTheme.typography.labelLarge,
        maxLines = if (compact) 1 else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
    val policy = state.preparation.presentation.invocationPolicy
    SessionActionButton(
        label = when (policy) {
            TranslationInvocationPolicy.Immediate -> stringResource(MR.strings.action_translate)
            is TranslationInvocationPolicy.ExplicitAction -> policy.label
        },
        onClick = onExecute,
        modifier = Modifier.fillMaxWidth(),
        compact = compact,
    )
}

@Composable
private fun SuccessContent(
    result: TranslationResult,
    sourceText: String?,
    expanded: Boolean,
    compact: Boolean,
    showExpand: Boolean,
    showLanguageChange: Boolean,
    showEngineChange: Boolean,
    showCopy: Boolean,
    onDismiss: (() -> Unit)?,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
    showResultLanguage: Boolean,
) {
    val showOverflowAction = showExpand && !expanded
    val languagePair = stringResource(
        MR.strings.translation_language_pair,
        result.sourceLanguage.displayName(),
        result.targetLanguage.displayName(),
    )
    val changeLanguages = {
        onExternalAction(
            TranslationSessionExternalAction.ChangeLanguages(
                source = result.sourceLanguage,
                target = result.targetLanguage,
            ),
        )
    }
    val changeEngine = {
        onExternalAction(TranslationSessionExternalAction.ChooseEngine)
    }
    val speechContent = sourceText?.takeIf { onSpeechToggle != null }?.let { exactSourceText ->
        TranslationResultSpeechContent(
            sourceText = exactSourceText,
            sourceTarget = TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Source,
                text = exactSourceText,
                language = result.sourceLanguage,
            ),
            targetTarget = TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Target,
                text = result.translatedText,
                language = result.targetLanguage,
            ),
            onToggle = requireNotNull(onSpeechToggle),
        )
    }
    if (!compact) {
        if (speechContent != null) {
            TranslationSpeechSection(
                title = stringResource(MR.strings.translation_original),
                language = result.sourceLanguage.displayName(),
                text = speechContent.sourceText,
                target = speechContent.sourceTarget,
                speechState = speechState,
                onSpeechToggle = speechContent.onToggle,
            )
            HorizontalDivider()
            TranslationSpeechSectionHeader(
                title = stringResource(MR.strings.translation_title),
                language = result.targetLanguage.displayName(),
                target = speechContent.targetTarget,
                speechState = speechState,
                onSpeechToggle = speechContent.onToggle,
            )
        } else if (showResultLanguage) {
            TranslationLanguagePair(
                languagePair = languagePair,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (compact) {
        TranslationCompactSuccessContent(
            result = result,
            languagePair = languagePair,
            sourceLanguage = result.sourceLanguage.displayName(),
            targetLanguage = result.targetLanguage.displayName(),
            sourceSpeechTarget = speechContent?.sourceTarget,
            targetSpeechTarget = speechContent?.targetTarget,
            speechState = speechState,
            onSpeechToggle = speechContent?.onToggle,
            expanded = expanded,
            showExpand = showExpand,
            showLanguageChange = showLanguageChange,
            showEngineChange = showEngineChange,
            showCopy = showCopy,
            onDismiss = onDismiss,
            onCopy = onCopy,
            onExpand = onExpand,
            onChangeLanguages = changeLanguages,
            onChangeEngine = changeEngine,
        )
    } else {
        SelectionContainer {
            Text(
                text = result.translatedText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        result.presentation.resultAttribution?.let { attribution ->
            Text(
                text = attribution.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showCopy || showOverflowAction || showLanguageChange || showEngineChange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (showCopy) {
                    IconButton(onClick = { onCopy(result.translatedText) }) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(MR.strings.copy),
                        )
                    }
                }
                if (showOverflowAction) {
                    IconButton(onClick = onExpand) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInFull,
                            contentDescription = stringResource(MR.strings.action_expand),
                        )
                    }
                }
                if (showLanguageChange) {
                    IconButton(
                        onClick = changeLanguages,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = stringResource(MR.strings.action_change_language),
                        )
                    }
                }
                if (showEngineChange) {
                    IconButton(onClick = changeEngine) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(MR.strings.translation_choose_engine),
                        )
                    }
                }
            }
        }
    }
}

private data class TranslationResultSpeechContent(
    val sourceText: String,
    val sourceTarget: TranslationResultSpeechTarget,
    val targetTarget: TranslationResultSpeechTarget,
    val onToggle: (TranslationResultSpeechTarget) -> Unit,
)

@Composable
internal fun TranslationLanguagePair(
    languagePair: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val text = remember(languagePair) {
        buildAnnotatedString {
            val arrowIndex = languagePair.indexOf(LANGUAGE_PAIR_ARROW)
            if (arrowIndex < 0) {
                append(languagePair)
            } else {
                append(languagePair, 0, arrowIndex)
                appendInlineContent(LANGUAGE_PAIR_ARROW_ID, LANGUAGE_PAIR_ARROW.toString())
                append(languagePair, arrowIndex + 1, languagePair.length)
            }
        }
    }
    Text(
        text = text,
        inlineContent = mapOf(
            LANGUAGE_PAIR_ARROW_ID to InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = color,
                )
            },
        ),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private const val LANGUAGE_PAIR_ARROW_ID = "language-pair-arrow"
private const val LANGUAGE_PAIR_ARROW = '→'

@Composable
internal fun TranslationCompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PreparationContent(
    preparation: TranslationPreparation,
    onRetry: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    useExternalEnginePicker: Boolean,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    compact: Boolean,
) {
    when (preparation) {
        is TranslationPreparation.ProviderDisclosureRequired -> {
            Text(
                text = preparation.disclosure.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = if (compact) 1 else Int.MAX_VALUE,
                overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
            )
            SessionMessage(preparation.disclosure.message, compact)
            SessionActionButton(
                label = preparation.disclosure.confirmationLabel,
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.ConfirmProviderDisclosure(
                            preparation.engine,
                            preparation.disclosure,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                compact = compact,
            )
            DocumentationAction(
                preparation.disclosure.documentationUrl ?: preparation.presentation.documentationUrl,
                onExternalAction,
                compact,
            )
        }
        is TranslationPreparation.ModelDownloadRequired -> {
            SessionMessage(stringResource(MR.strings.translation_models_required), compact)
            if (!compact) {
                preparation.models.forEach { model ->
                    Text("• ${model.displayName}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            SessionActionButton(
                label = stringResource(MR.strings.action_download),
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.DownloadModels(
                            preparation.engine,
                            preparation.models,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                compact = compact,
            )
            DocumentationAction(preparation.presentation.documentationUrl, onExternalAction, compact)
        }
        is TranslationPreparation.SystemSetupRequired -> {
            SessionMessage(preparation.reason.message(), compact)
            SessionActionButton(
                label = stringResource(MR.strings.action_settings),
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.OpenSetup(preparation.engine))
                },
                modifier = Modifier.fillMaxWidth(),
                compact = compact,
            )
            DocumentationAction(preparation.presentation.documentationUrl, onExternalAction, compact)
        }
        is TranslationPreparation.SetupInProgress -> {
            SessionMessage(stringResource(MR.strings.translation_setup_in_progress), compact)
            val progress = preparation.progress
            val total = progress?.total
            if (progress != null && total != null && total > 0) {
                LinearProgressIndicator(
                    progress = { progress.completed.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        is TranslationPreparation.SourceUndetermined -> {
            SessionMessage(stringResource(MR.strings.translation_source_undetermined), compact)
            if (!compact) {
                preparation.suggestedLanguages.forEach { language ->
                    TextButton(
                        onClick = { onSelectSource(language) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(language.displayName())
                    }
                }
            }
            SessionActionButton(
                label = stringResource(MR.strings.translation_choose_source_language),
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage)
                },
                modifier = Modifier.fillMaxWidth(),
                compact = compact,
            )
        }
        is TranslationPreparation.TargetLanguageRequired -> {
            SessionMessage(
                text = when (preparation.reason) {
                    TranslationTargetChoiceReason.NoDefaultTarget ->
                        stringResource(MR.strings.translation_target_required)
                    TranslationTargetChoiceReason.SourceEqualsTarget ->
                        stringResource(MR.strings.translation_source_equals_target)
                },
                compact = compact,
            )
            SessionActionButton(
                label = stringResource(MR.strings.translation_choose_target_language),
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage)
                },
                modifier = Modifier.fillMaxWidth(),
                compact = compact,
            )
        }
        is TranslationPreparation.EngineChoiceRequired -> {
            SessionMessage(
                text = stringResource(
                    when (preparation.reason) {
                        TranslationEngineChoiceReason.NoEngineConfigured ->
                            MR.strings.translation_engine_not_configured
                        is TranslationEngineChoiceReason.SelectedEngineUnavailable ->
                            MR.strings.translation_selected_engine_unavailable
                    },
                ),
                compact = compact,
            )
            if (compact || useExternalEnginePicker) {
                SessionActionButton(
                    label = stringResource(MR.strings.translation_choose_engine),
                    onClick = {
                        onExternalAction(TranslationSessionExternalAction.ChooseEngine)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    compact = compact,
                )
            } else {
                preparation.engines.forEach { engine ->
                    EngineChoice(engine, onSelectEngine)
                }
            }
        }
        is TranslationPreparation.Unavailable -> {
            SessionMessage(preparation.reason.message(), compact)
            val unsupportedPair = preparation.reason as? TranslationUnavailableReason.UnsupportedLanguagePair
            if (unsupportedPair != null) {
                SessionActionButton(
                    label = stringResource(MR.strings.translation_change_languages),
                    onClick = {
                        onExternalAction(
                            TranslationSessionExternalAction.ChangeLanguages(
                                source = unsupportedPair.source,
                                target = unsupportedPair.target,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    compact = compact,
                )
            } else {
                SessionActionButton(
                    label = stringResource(MR.strings.action_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    compact = compact,
                )
            }
        }
        is TranslationPreparation.Rejected -> {
            SessionMessage(preparation.reason.message(), compact)
        }
        is TranslationPreparation.Ready -> error("Ready preparation must use TranslationSessionState.Ready")
    }
}

@Composable
private fun EngineChoice(
    engine: KnownTranslationEngine,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
) {
    val availability = engine.buildAvailability
    TextButton(
        onClick = {
            onSelectEngine(TranslationEngineSelection.Explicit(engine.id))
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = availability is TranslationEngineBuildAvailability.Included,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(engine.engineName)
            Text(
                text = when (availability) {
                    TranslationEngineBuildAvailability.Included -> engine.providerName
                    is TranslationEngineBuildAvailability.NotIncluded -> availability.reason
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FailedContent(
    state: TranslationSessionState.Failed,
    onRetry: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    compact: Boolean,
) {
    SessionMessage(
        text = when (val failure = state.failure) {
            TranslationSessionFailure.UnexpectedPreparationFailure,
            TranslationSessionFailure.UnexpectedExecutionFailure,
            -> stringResource(MR.strings.translation_failed)
            TranslationSessionFailure.PreparationTimedOut,
            TranslationSessionFailure.ExecutionTimedOut,
            ->
                stringResource(MR.strings.translation_timed_out)
            is TranslationSessionFailure.ExecutionFailure -> {
                when (val reason = failure.execution.reason) {
                    TranslationFailureReason.InvalidReadyTranslation ->
                        stringResource(MR.strings.translation_invalid_session)
                    is TranslationFailureReason.ProviderFailure ->
                        reason.message?.takeIf(String::isNotBlank)
                            ?: stringResource(MR.strings.translation_failed)
                }
            }
        },
        compact = compact,
    )
    SessionActionButton(
        label = stringResource(MR.strings.action_retry),
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
        compact = compact,
    )
    DocumentationAction(state.presentation?.documentationUrl, onExternalAction, compact)
}

@Composable
private fun DocumentationAction(
    url: String?,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    compact: Boolean = false,
) {
    if (url == null || compact) return
    TextButton(
        onClick = {
            onExternalAction(TranslationSessionExternalAction.OpenDocumentation(url))
        },
    ) {
        Text(stringResource(MR.strings.action_learn_more))
    }
}

@Composable
private fun SessionMessage(
    text: String,
    compact: Boolean,
) {
    Text(
        text = text,
        maxLines = if (compact) COMPACT_MESSAGE_MAX_LINES else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
private fun SessionActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    compact: Boolean,
) {
    if (compact) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = COMPACT_ACTION_MINIMUM_HEIGHT),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(label)
        }
    }
}

private fun TranslationSessionState.Active.contextualDocumentationUrl(): String? {
    return when (this) {
        is TranslationSessionState.PreparationRequired -> preparation.contextualDocumentationUrl()
        is TranslationSessionState.Failed -> presentation?.documentationUrl
        is TranslationSessionState.Settling,
        is TranslationSessionState.Preparing,
        is TranslationSessionState.Ready,
        is TranslationSessionState.Translating,
        is TranslationSessionState.Success,
        is TranslationSessionState.ProviderSurfaceOpened,
        -> null
    }
}

private fun TranslationPreparation.contextualDocumentationUrl(): String? {
    return when (this) {
        is TranslationPreparation.ProviderDisclosureRequired ->
            disclosure.documentationUrl ?: presentation.documentationUrl
        is TranslationPreparation.ModelDownloadRequired -> presentation.documentationUrl
        is TranslationPreparation.SystemSetupRequired -> presentation.documentationUrl
        is TranslationPreparation.Ready,
        is TranslationPreparation.SetupInProgress,
        is TranslationPreparation.SourceUndetermined,
        is TranslationPreparation.TargetLanguageRequired,
        is TranslationPreparation.EngineChoiceRequired,
        is TranslationPreparation.Unavailable,
        is TranslationPreparation.Rejected,
        -> null
    }
}

private fun TranslationSessionState.Active.hasExpandedCompactContent(): Boolean {
    return when (this) {
        is TranslationSessionState.Ready,
        is TranslationSessionState.PreparationRequired,
        is TranslationSessionState.ProviderSurfaceOpened,
        is TranslationSessionState.Failed,
        -> true
        is TranslationSessionState.Settling,
        is TranslationSessionState.Preparing,
        is TranslationSessionState.Translating,
        is TranslationSessionState.Success,
        -> false
    }
}

private const val COMPACT_MESSAGE_MAX_LINES = 3
private val COMPACT_ACTION_MINIMUM_HEIGHT = 48.dp

@Composable
private fun TranslationSystemSetupReason.message(): String {
    return when (this) {
        TranslationSystemSetupReason.ServiceDisabled ->
            stringResource(MR.strings.translation_service_disabled)
        TranslationSystemSetupReason.LanguageModelsRequired ->
            stringResource(MR.strings.translation_system_setup_required)
        is TranslationSystemSetupReason.ProviderActionRequired ->
            stringResource(MR.strings.translation_provider_action_required, description)
    }
}

@Composable
private fun TranslationUnavailableReason.message(): String {
    return when (this) {
        is TranslationUnavailableReason.UnsupportedOs ->
            stringResource(MR.strings.translation_unsupported_os, minimumApi)
        TranslationUnavailableReason.ServiceMissing ->
            stringResource(MR.strings.translation_service_missing)
        TranslationUnavailableReason.SystemSettingsUnavailable ->
            stringResource(MR.strings.translation_settings_unavailable)
        is TranslationUnavailableReason.UnsupportedLanguage ->
            stringResource(MR.strings.translation_unsupported_language, language.displayName())
        is TranslationUnavailableReason.UnsupportedLanguagePair ->
            stringResource(
                MR.strings.translation_unsupported_pair,
                source.displayName(),
                target.displayName(),
            )
        is TranslationUnavailableReason.EngineUnavailable ->
            stringResource(MR.strings.translation_engine_unavailable, engine.value, reason)
    }
}

@Composable
private fun TranslationRejectionReason.message(): String {
    return when (this) {
        TranslationRejectionReason.BlankInput -> stringResource(MR.strings.translation_blank_input)
        is TranslationRejectionReason.InputTooLarge -> stringResource(
            MR.strings.translation_input_too_large,
            actualCodePoints,
            maximumCodePoints,
        )
    }
}

private fun LanguageTag.displayName(): String {
    return Locale.forLanguageTag(value)
        .getDisplayName(Locale.getDefault())
        .ifBlank { value }
}

internal const val TRANSLATION_SESSION_PROGRESS_TAG = "translation_session_progress"
internal const val TRANSLATION_SESSION_CONTENT_TAG = "translation_session_content"
