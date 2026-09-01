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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedResult
import mihon.translation.ui.session.isTranslationInProgress
import tachiyomi.i18n.MR
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
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
            .heightIn(
                min = WORKBENCH_PANE_MINIMUM_HEIGHT,
                max = WORKBENCH_PANE_MAXIMUM_HEIGHT,
            )
            .padding(WORKBENCH_PADDING),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    innerTextField()
                }
            },
        )
        TranslationWorkbenchActions(
            speechTarget = speechTarget,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
        ) {
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.translator_clear_input),
                    )
                }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = WORKBENCH_PANE_MINIMUM_HEIGHT,
                max = WORKBENCH_PANE_MAXIMUM_HEIGHT,
            )
            .padding(WORKBENCH_PADDING),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            val active = session as? TranslationSessionState.Active
            if (active == null) {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineSmall,
                )
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
                IconButton(onClick = { onCopy(resultText) }) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(MR.strings.copy),
                    )
                }
            }
            if (resultText != null && onShare != null) {
                IconButton(onClick = { onShare(resultText) }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(MR.strings.action_share),
                    )
                }
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
            Spacer(modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        trailingContent()
    }
}

private val WORKBENCH_PANE_MINIMUM_HEIGHT = 184.dp
private val WORKBENCH_PANE_MAXIMUM_HEIGHT = 280.dp
private val WORKBENCH_PADDING = 20.dp
