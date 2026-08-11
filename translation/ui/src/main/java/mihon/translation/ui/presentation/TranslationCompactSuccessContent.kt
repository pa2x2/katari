package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.translation.api.result.TranslationResult
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationCompactSuccessContent(
    result: TranslationResult,
    languagePair: String,
    sourceLanguage: String,
    targetLanguage: String,
    sourceSpeechTarget: TranslationResultSpeechTarget?,
    targetSpeechTarget: TranslationResultSpeechTarget?,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
    expanded: Boolean,
    showExpand: Boolean,
    showLanguageChange: Boolean,
    showEngineChange: Boolean,
    showCopy: Boolean,
    onDismiss: (() -> Unit)?,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onChangeLanguages: () -> Unit,
    onChangeEngine: () -> Unit,
) {
    var resultOverflowed by remember(result.translatedText) { mutableStateOf(false) }
    val showOverflowAction = showExpand && !expanded && resultOverflowed

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = result.translatedText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = ANCHORED_RESULT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { resultOverflowed = it.hasVisualOverflow },
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

    result.presentation.resultAttribution?.let { attribution ->
        Text(
            text = attribution.label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (sourceSpeechTarget != null && targetSpeechTarget != null && onSpeechToggle != null) {
                TranslationCompactSpeechLanguagePair(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    sourceTarget = sourceSpeechTarget,
                    targetTarget = targetSpeechTarget,
                    speechState = speechState,
                    onSpeechToggle = onSpeechToggle,
                    modifier = Modifier.widthIn(max = COMPACT_SPEECH_LANGUAGE_PAIR_MAXIMUM_WIDTH),
                )
            } else {
                TranslationLanguagePair(
                    languagePair = languagePair,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
        if (showLanguageChange || showEngineChange) {
            TranslationCompactMoreMenu(
                showLanguageChange = showLanguageChange,
                showEngineChange = showEngineChange,
                onChangeLanguages = onChangeLanguages,
                onChangeEngine = onChangeEngine,
            )
        }
    }
}

@Composable
private fun TranslationCompactSpeechLanguagePair(
    sourceLanguage: String,
    targetLanguage: String,
    sourceTarget: TranslationResultSpeechTarget,
    targetTarget: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TranslationSpeechActionButton(sourceTarget, speechState, onSpeechToggle, compact = true)
        Text(
            text = sourceLanguage,
            modifier = Modifier.widthIn(max = COMPACT_LANGUAGE_MAXIMUM_WIDTH),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TranslationSpeechActionButton(targetTarget, speechState, onSpeechToggle, compact = true)
        Text(
            text = targetLanguage,
            modifier = Modifier.widthIn(max = COMPACT_LANGUAGE_MAXIMUM_WIDTH),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TranslationCompactMoreMenu(
    showLanguageChange: Boolean,
    showEngineChange: Boolean,
    onChangeLanguages: () -> Unit,
    onChangeEngine: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        TranslationCompactIconButton(
            icon = Icons.Outlined.MoreVert,
            contentDescription = stringResource(MR.strings.label_more),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (showLanguageChange) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_change_language)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onChangeLanguages()
                    },
                )
            }
            if (showEngineChange) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.translation_choose_engine)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onChangeEngine()
                    },
                )
            }
        }
    }
}

private const val ANCHORED_RESULT_MAX_LINES = 5
private val COMPACT_LANGUAGE_MAXIMUM_WIDTH = 80.dp
private val COMPACT_SPEECH_LANGUAGE_PAIR_MAXIMUM_WIDTH = 220.dp
