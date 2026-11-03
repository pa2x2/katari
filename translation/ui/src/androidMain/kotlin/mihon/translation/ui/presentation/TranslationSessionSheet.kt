package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.sheet.Sheet
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.ui.presentation.content.TranslationSessionContent
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.language.TranslationLanguageSuggestions

/**
 * The translation in a sheet, used when there is no selection to anchor a popup to, when the popup does not fit, and
 * for fullscreen. With [onCollapse], dismissing the sheet returns to the popup instead of ending the translation.
 */
@Composable
internal fun TranslationSessionSheet(
    state: TranslationSessionState.Active,
    onDismiss: () -> Unit,
    onCollapse: (() -> Unit)?,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectTarget: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    languageSuggestions: TranslationLanguageSuggestions?,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
) {
    val onDismissRequest = onCollapse ?: onDismiss
    Sheet(onDismissRequest = onDismissRequest) {
        TranslationSessionContent(
            state = state,
            expanded = true,
            showHeader = true,
            showExpand = false,
            showLanguageChange = true,
            showEngineChange = true,
            showCopy = true,
            useExternalEnginePicker = false,
            onDismiss = onDismiss,
            onExecute = onExecute,
            onRetry = onRetry,
            onCopy = onCopy,
            onExpand = onExpand,
            onSelectSource = onSelectSource,
            onSelectEngine = onSelectEngine,
            onExternalAction = onExternalAction,
            onCollapse = onCollapse,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
            languageSuggestions = languageSuggestions,
            onSelectTarget = onSelectTarget,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
    }
}
