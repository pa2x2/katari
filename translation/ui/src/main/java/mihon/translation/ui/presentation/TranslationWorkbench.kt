package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.Share
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.lineHeight
import mihon.presentation.kit.field.PlainTextField
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.GroupCard
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.ui.presentation.content.TranslationSessionContent
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedResult
import mihon.translation.ui.session.isTranslationInProgress
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TranslationWorkbench(
    text: String,
    session: TranslationSessionState,
    sourceSpeechTarget: TranslationResultSpeechTarget?,
    speechState: TranslationResultSpeechState,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    onCopy: ((String) -> Unit)? = null,
    onShare: ((String) -> Unit)? = null,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    modifier: Modifier = Modifier,
    inputPlaceholder: String = stringResource(MR.strings.translator_input_label),
    outputPlaceholder: String = stringResource(MR.strings.translator_output_label),
) {
    val result = session.displayedResult()
    val targetSpeechTarget = result?.let {
        TranslationResultSpeechTarget(
            side = TranslationResultSpeechSide.Target,
            text = it.translatedText,
            language = it.targetLanguage,
        )
    }
    GroupCard(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.High) {
        Column {
            TranslationSourcePane(
                text = text,
                placeholder = inputPlaceholder,
                speechTarget = sourceSpeechTarget,
                speechState = speechState,
                onTextChange = onTextChange,
                onClear = onClear,
                onSpeechToggle = onSpeechToggle,
            )
            Box {
                HorizontalDivider()
                if (session.isTranslationInProgress()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            TranslationOutputPane(
                session = session,
                placeholder = outputPlaceholder,
                resultText = result?.translatedText,
                speechTarget = targetSpeechTarget,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
                onCopy = onCopy,
                onShare = onShare,
                onExecute = onExecute,
                onRetry = onRetry,
                onSelectSource = onSelectSource,
                onSelectEngine = onSelectEngine,
                onExternalAction = onExternalAction,
            )
        }
    }
}

@Composable
private fun TranslationSourcePane(
    text: String,
    placeholder: String,
    speechTarget: TranslationResultSpeechTarget?,
    speechState: TranslationResultSpeechState,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        val state = rememberTextFieldState(text)
        val currentText by rememberUpdatedState(text)
        val currentOnTextChange by rememberUpdatedState(onTextChange)
        LaunchedEffect(text) {
            if (state.text.toString() != text) {
                state.edit { replace(0, length, text) }
            }
        }
        LaunchedEffect(state) {
            snapshotFlow { state.text.toString() }.collect {
                if (it != currentText) currentOnTextChange(it)
            }
        }
        PlainTextField(
            state = state,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth(),
            role = WORKBENCH_TEXT_ROLE,
            minLines = WORKBENCH_PANE_MINIMUM_LINES,
            maxLines = WORKBENCH_PANE_MAXIMUM_LINES,
        )
        TranslationWorkbenchActions(
            speechTarget = speechTarget,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
        ) {
            if (text.isNotEmpty()) {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.translator_clear_input),
                    onClick = onClear,
                )
            }
        }
    }
}

@Composable
private fun TranslationOutputPane(
    session: TranslationSessionState,
    placeholder: String,
    resultText: String?,
    speechTarget: TranslationResultSpeechTarget?,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    onCopy: ((String) -> Unit)?,
    onShare: ((String) -> Unit)?,
    onExecute: () -> Unit,
    onRetry: () -> Unit,
    onSelectSource: (LanguageTag) -> Unit,
    onSelectEngine: (TranslationEngineSelection) -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
) {
    val lineHeight = WORKBENCH_TEXT_ROLE.lineHeight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        // Sized in lines of the source pane's text, so both panes line up at any font scale.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = lineHeight * WORKBENCH_PANE_MINIMUM_LINES,
                    max = lineHeight * WORKBENCH_PANE_MAXIMUM_LINES,
                )
                .verticalScroll(rememberScrollState()),
        ) {
            val active = session as? TranslationSessionState.Active
            if (active == null) {
                Text(text = placeholder, role = WORKBENCH_TEXT_ROLE, tone = ContentTone.Secondary)
            } else {
                TranslationSessionContent(
                    state = active,
                    expanded = true,
                    showHeader = false,
                    showExpand = false,
                    showLanguageChange = false,
                    showEngineChange = false,
                    showCopy = false,
                    useExternalEnginePicker = true,
                    onDismiss = {},
                    onExecute = onExecute,
                    onRetry = onRetry,
                    onCopy = {},
                    onExpand = {},
                    onSelectSource = onSelectSource,
                    onSelectEngine = onSelectEngine,
                    onExternalAction = onExternalAction,
                    showProgress = false,
                    showResultLanguage = false,
                    flushContent = true,
                )
            }
        }
        TranslationWorkbenchActions(
            speechTarget = speechTarget,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
        ) {
            if (resultText != null && onCopy != null) {
                IconButton(
                    icon = MaterialSymbols.Rounded.ContentCopy,
                    contentDescription = stringResource(MR.strings.copy),
                    onClick = { onCopy(resultText) },
                )
            }
            if (resultText != null && onShare != null) {
                IconButton(
                    icon = MaterialSymbols.Rounded.Share,
                    contentDescription = stringResource(MR.strings.action_share),
                    onClick = { onShare(resultText) },
                )
            }
        }
    }
}

@Composable
private fun TranslationWorkbenchActions(
    speechTarget: TranslationResultSpeechTarget?,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    trailingContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (speechTarget != null) {
            TranslationSpeechActionButton(
                target = speechTarget,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
            )
        } else {
            Spacer(modifier = Modifier.size(KatariTheme.sizes.touchTarget))
        }
        Spacer(modifier = Modifier.weight(1f))
        trailingContent()
    }
}

// Text being translated is set large, at the size of a sheet title.
private val WORKBENCH_TEXT_ROLE = TextRole.SheetTitle
private const val WORKBENCH_PANE_MINIMUM_LINES = 3
private const val WORKBENCH_PANE_MAXIMUM_LINES = 6
