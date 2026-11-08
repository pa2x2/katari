package app.katari.translation.ui.presentation

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.surface.FloatingPanel
import app.katari.translation.api.engine.TranslationEngineSelection
import app.katari.translation.ui.presentation.content.TranslationSessionContent
import app.katari.translation.ui.session.TranslationSessionState
import app.katari.translation.ui.session.language.TranslationLanguageSuggestions

/**
 * The compact translation beside the selected text. While [visible] is false it is still measured, so the position
 * provider can decide whether it fits, but it is invisible and hidden from accessibility services.
 */
@Composable
internal fun TranslationSessionPopup(
    positionProvider: PopupPositionProvider,
    state: TranslationSessionState.Active,
    width: Dp,
    visible: Boolean,
    onDismiss: () -> Unit,
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
    FloatingPanel(
        positionProvider = positionProvider,
        modifier = Modifier
            .alpha(if (visible) 1f else 0f)
            .width(width)
            .then(if (visible) Modifier else Modifier.semantics { hideFromAccessibility() }),
    ) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = onDismiss,
        )
        TranslationSessionContent(
            state = state,
            expanded = false,
            showHeader = true,
            showExpand = true,
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
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
            languageSuggestions = languageSuggestions,
            onSelectTarget = onSelectTarget,
            compact = true,
        )
    }
}
