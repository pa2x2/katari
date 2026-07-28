package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationFailureReason
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationRejectionReason
import mihon.translation.api.TranslationResult
import mihon.translation.api.TranslationSystemSetupReason
import mihon.translation.api.TranslationTargetChoiceReason
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.ui.session.TranslationSessionFailure
import mihon.translation.ui.session.TranslationSessionState
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
    showCopy: Boolean,
    useExternalEnginePicker: Boolean,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (TranslationLanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val inProgress = state is TranslationSessionState.Settling ||
        state is TranslationSessionState.Preparing ||
        state is TranslationSessionState.Translating
    val displayedResult = when (state) {
        is TranslationSessionState.Settling -> state.previousResult
        is TranslationSessionState.Preparing -> state.previousResult
        is TranslationSessionState.Translating -> state.previousResult
        is TranslationSessionState.Success -> state.result
        is TranslationSessionState.Ready,
        is TranslationSessionState.PreparationRequired,
        is TranslationSessionState.Failed,
        -> null
    }

    Column(modifier = modifier.testTag(TRANSLATION_SESSION_CONTENT_TAG)) {
        if (inProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TRANSLATION_SESSION_PROGRESS_TAG),
            )
        }
        val compactResult = compact && displayedResult != null
        if (showHeader && !compactResult) {
            TranslationSessionHeader(
                title = stringResource(MR.strings.translation_title),
                onDismiss = onDismiss,
                compact = compact,
            )
            HorizontalDivider()
        }
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 16.dp else 20.dp,
                vertical = if (compact) 12.dp else 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            if (displayedResult != null) {
                SuccessContent(
                    result = displayedResult,
                    expanded = expanded,
                    compact = compact,
                    showExpand = showExpand,
                    showLanguageChange = showLanguageChange,
                    showCopy = showCopy,
                    onDismiss = if (compactResult) onDismiss else null,
                    onCopy = onCopy,
                    onExpand = onExpand,
                    onExternalAction = onExternalAction,
                )
            } else {
                when (state) {
                    is TranslationSessionState.Settling,
                    is TranslationSessionState.Preparing,
                    is TranslationSessionState.Translating,
                    is TranslationSessionState.Success,
                    -> Unit
                    is TranslationSessionState.Ready -> ReadyContent(state, onExecute)
                    is TranslationSessionState.PreparationRequired -> PreparationContent(
                        preparation = state.preparation,
                        onRetry = onRetry,
                        onSelectSource = onSelectSource,
                        onSelectEngine = onSelectEngine,
                        useExternalEnginePicker = useExternalEnginePicker,
                        onExternalAction = onExternalAction,
                    )
                    is TranslationSessionState.Failed -> FailedContent(state, onRetry, onExternalAction)
                }
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
) {
    Text(
        text = state.preparation.presentation.engineName,
        style = MaterialTheme.typography.labelLarge,
    )
    Button(
        onClick = onExecute,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val policy = state.preparation.presentation.invocationPolicy
        Text(
            when (policy) {
                TranslationInvocationPolicy.Immediate -> stringResource(MR.strings.action_translate)
                is TranslationInvocationPolicy.ExplicitAction -> policy.label
            },
        )
    }
}

@Composable
private fun SuccessContent(
    result: TranslationResult,
    expanded: Boolean,
    compact: Boolean,
    showExpand: Boolean,
    showLanguageChange: Boolean,
    showCopy: Boolean,
    onDismiss: (() -> Unit)?,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    var resultOverflowed by remember(result.translatedText, compact) { mutableStateOf(false) }
    val showOverflowAction = showExpand && !expanded && (!compact || resultOverflowed)
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
    if (!compact) {
        Text(
            text = languagePair,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = languagePair,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showCopy) {
                TranslationCompactIconButton(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.copy),
                    onClick = { onCopy(result.translatedText) },
                )
            }
            if (showOverflowAction) {
                TranslationCompactIconButton(
                    icon = Icons.Outlined.OpenInFull,
                    contentDescription = stringResource(MR.strings.action_expand),
                    onClick = onExpand,
                )
            }
            if (showLanguageChange) {
                TranslationCompactIconButton(
                    icon = Icons.Outlined.Language,
                    contentDescription = stringResource(MR.strings.action_change_language),
                    onClick = changeLanguages,
                )
            }
            onDismiss?.let {
                TranslationCompactIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(MR.strings.action_close),
                    onClick = it,
                )
            }
        }
    }
    SelectionContainer {
        Text(
            text = result.translatedText,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (compact) ANCHORED_RESULT_MAX_LINES else Int.MAX_VALUE,
            overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
            onTextLayout = { resultOverflowed = it.hasVisualOverflow },
        )
    }
    result.presentation.resultAttribution?.let { attribution ->
        Text(
            text = attribution.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!compact && (showCopy || showOverflowAction || showLanguageChange)) {
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
        }
    }
    DocumentationAction(result.presentation.documentationUrl, onExternalAction)
}

private const val ANCHORED_RESULT_MAX_LINES = 5

@Composable
private fun TranslationCompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PreparationContent(
    preparation: TranslationPreparation,
    onRetry: () -> Unit,
    onSelectSource: (TranslationLanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    useExternalEnginePicker: Boolean,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    when (preparation) {
        is TranslationPreparation.ProviderDisclosureRequired -> {
            Text(preparation.disclosure.title, style = MaterialTheme.typography.titleSmall)
            Text(preparation.disclosure.message)
            Button(
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.ConfirmProviderDisclosure(
                            preparation.engine,
                            preparation.disclosure,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(preparation.disclosure.confirmationLabel)
            }
            DocumentationAction(
                preparation.disclosure.documentationUrl ?: preparation.presentation.documentationUrl,
                onExternalAction,
            )
        }
        is TranslationPreparation.ModelDownloadRequired -> {
            Text(stringResource(MR.strings.translation_models_required))
            preparation.models.forEach { model ->
                Text("• ${model.displayName}", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.DownloadModels(
                            preparation.engine,
                            preparation.models,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.action_download))
            }
            DocumentationAction(preparation.presentation.documentationUrl, onExternalAction)
        }
        is TranslationPreparation.SystemSetupRequired -> {
            Text(preparation.reason.message())
            Button(
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.OpenSystemSetup(preparation.engine))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.action_settings))
            }
            DocumentationAction(preparation.presentation.documentationUrl, onExternalAction)
        }
        is TranslationPreparation.SetupInProgress -> {
            Text(stringResource(MR.strings.translation_setup_in_progress))
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
            Text(stringResource(MR.strings.translation_source_undetermined))
            preparation.suggestedLanguages.forEach { language ->
                TextButton(
                    onClick = { onSelectSource(language) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(language.displayName())
                }
            }
            Button(
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.translation_choose_source_language))
            }
        }
        is TranslationPreparation.TargetLanguageRequired -> {
            Text(
                when (preparation.reason) {
                    TranslationTargetChoiceReason.NoDefaultTarget ->
                        stringResource(MR.strings.translation_target_required)
                    TranslationTargetChoiceReason.SourceEqualsTarget ->
                        stringResource(MR.strings.translation_source_equals_target)
                },
            )
            Button(
                onClick = {
                    onExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.translation_choose_target_language))
            }
        }
        is TranslationPreparation.EngineChoiceRequired -> {
            Text(
                stringResource(MR.strings.translation_selected_engine_unavailable),
            )
            if (useExternalEnginePicker) {
                Button(
                    onClick = {
                        onExternalAction(TranslationSessionExternalAction.ChooseEngine)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(MR.strings.translation_choose_engine))
                }
            } else {
                preparation.engines.forEach { engine ->
                    EngineChoice(engine, onSelectEngine)
                }
            }
        }
        is TranslationPreparation.Unavailable -> {
            Text(preparation.reason.message())
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(MR.strings.action_retry))
            }
        }
        is TranslationPreparation.Rejected -> {
            Text(preparation.reason.message())
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
) {
    Text(
        when (val failure = state.failure) {
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
    )
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(MR.strings.action_retry))
    }
    DocumentationAction(state.presentation?.documentationUrl, onExternalAction)
}

@Composable
private fun DocumentationAction(
    url: String?,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    if (url == null) return
    TextButton(
        onClick = {
            onExternalAction(TranslationSessionExternalAction.OpenDocumentation(url))
        },
    ) {
        Text(stringResource(MR.strings.action_learn_more))
    }
}

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

private fun TranslationLanguageTag.displayName(): String {
    return Locale.forLanguageTag(value)
        .getDisplayName(Locale.getDefault())
        .ifBlank { value }
}

internal const val TRANSLATION_SESSION_PROGRESS_TAG = "translation_session_progress"
internal const val TRANSLATION_SESSION_CONTENT_TAG = "translation_session_content"
