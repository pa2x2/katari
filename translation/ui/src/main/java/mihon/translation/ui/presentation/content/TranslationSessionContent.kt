package mihon.translation.ui.presentation.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedSessionResult
import mihon.translation.ui.session.language.TranslationLanguageSuggestions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One translation session as the popup, the sheet, the workbench and embedded panels show it: an optional header,
 * then the result and the content for the session's state. [compact] is the anchored popup's condensed layout.
 */
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
    onCollapse: (() -> Unit)? = null,
    speechState: TranslationResultSpeechState = TranslationResultSpeechState(),
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)? = null,
    modifier: Modifier = Modifier,
    languageSuggestions: TranslationLanguageSuggestions? = null,
    onSelectTarget: (LanguageTag) -> Unit = {},
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
    val spacing = KatariTheme.spacing
    Column(
        modifier = modifier
            .then(
                if (compactResult) {
                    Modifier.width(IntrinsicSize.Max)
                } else {
                    Modifier.fillMaxWidth()
                },
            ),
    ) {
        if (showProgress && inProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (showHeader && !compactResult) {
            val documentationUrl = state.contextualDocumentationUrl().takeIf { compact }
            TranslationSessionHeader(
                title = stringResource(MR.strings.translation_title),
                onDismiss = onDismiss,
                compact = compact,
                onExpand = onExpand.takeIf { compact && showExpand },
                onCollapse = onCollapse,
                onDocumentation = documentationUrl?.let { url ->
                    {
                        onExternalAction(TranslationSessionExternalAction.OpenDocumentation(url))
                    }
                },
            )
        }
        Column(
            modifier = if (flushContent) {
                Modifier
            } else {
                Modifier.padding(
                    horizontal = if (compact) spacing.l else spacing.rowInset,
                    vertical = if (compact) spacing.m else spacing.xl,
                )
            },
            verticalArrangement = Arrangement.spacedBy(
                when {
                    compactResult -> spacing.xs
                    compact -> spacing.s
                    else -> spacing.m
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
                    suggestions = languageSuggestions?.takeIf { it.preparation == state.preparation },
                    onRetry = onRetry,
                    onSelectSource = onSelectSource,
                    onSelectTarget = onSelectTarget,
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
