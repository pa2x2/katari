package mihon.translation.ui.presentation.content

import androidx.compose.runtime.Composable
import mihon.translation.api.result.TranslationFailureReason
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.TranslationSessionFailure
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FailedContent(
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
    )
    DocumentationAction(state.presentation?.documentationUrl, onExternalAction, compact)
}
