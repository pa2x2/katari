package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.translation_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.action_close),
                    )
                }
            }
            HorizontalDivider()
        }
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (displayedResult != null) {
                SuccessContent(
                    result = displayedResult,
                    expanded = expanded,
                    showExpand = showExpand,
                    showLanguageChange = showLanguageChange,
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
    showExpand: Boolean,
    showLanguageChange: Boolean,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    Text(
        text = stringResource(
            MR.strings.translation_language_pair,
            result.sourceLanguage.displayName(),
            result.targetLanguage.displayName(),
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SelectionContainer {
        Text(
            text = result.translatedText,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(onClick = { onCopy(result.translatedText) }) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(MR.strings.copy),
            )
        }
        if (showExpand && !expanded) {
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Outlined.OpenInFull,
                    contentDescription = stringResource(MR.strings.action_expand),
                )
            }
        }
        if (showLanguageChange) {
            IconButton(
                onClick = {
                    onExternalAction(
                        TranslationSessionExternalAction.ChangeLanguages(
                            source = result.sourceLanguage,
                            target = result.targetLanguage,
                        ),
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = stringResource(MR.strings.action_change_language),
                )
            }
        }
    }
    DocumentationAction(result.presentation.documentationUrl, onExternalAction)
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
