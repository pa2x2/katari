package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
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
import mihon.translation.ui.presentation.language.TranslationLanguageArrow
import mihon.translation.ui.presentation.language.TranslationLanguageChipPair
import mihon.translation.ui.presentation.language.TranslationSourceLanguageChip
import mihon.translation.ui.presentation.language.TranslationTargetLanguageChip
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalLayoutApi::class)
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
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
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

    // Changeable languages are wider than their names, so the actions move below them when both do not fit.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (sourceSpeechTarget != null && targetSpeechTarget != null && onSpeechToggle != null) {
            TranslationCompactSpeechLanguagePair(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                sourceTarget = sourceSpeechTarget,
                targetTarget = targetSpeechTarget,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
                showLanguageChange = showLanguageChange,
                onChooseSource = onChooseSource,
                onChooseTarget = onChooseTarget,
                modifier = if (showLanguageChange) {
                    Modifier
                } else {
                    Modifier.widthIn(max = COMPACT_SPEECH_LANGUAGE_PAIR_MAXIMUM_WIDTH)
                },
            )
        } else if (showLanguageChange) {
            TranslationLanguageChipPair(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                onChooseSource = onChooseSource,
                onChooseTarget = onChooseTarget,
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
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TranslationCompactActions(
                result = result,
                showCopy = showCopy,
                showOverflowAction = showOverflowAction,
                showEngineChange = showEngineChange,
                onCopy = onCopy,
                onExpand = onExpand,
                onChangeEngine = onChangeEngine,
            )
        }
    }
}

@Composable
private fun TranslationCompactActions(
    result: TranslationResult,
    showCopy: Boolean,
    showOverflowAction: Boolean,
    showEngineChange: Boolean,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onChangeEngine: () -> Unit,
) {
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
    if (showEngineChange) {
        TranslationCompactMoreMenu(onChangeEngine = onChangeEngine)
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
    showLanguageChange: Boolean,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TranslationSpeechActionButton(sourceTarget, speechState, onSpeechToggle, compact = true)
        if (showLanguageChange) {
            TranslationSourceLanguageChip(sourceLanguage, onChooseSource)
        } else {
            TranslationCompactLanguageName(sourceLanguage)
        }
        TranslationLanguageArrow()
        TranslationSpeechActionButton(targetTarget, speechState, onSpeechToggle, compact = true)
        if (showLanguageChange) {
            TranslationTargetLanguageChip(targetLanguage, onChooseTarget)
        } else {
            TranslationCompactLanguageName(targetLanguage)
        }
    }
}

@Composable
private fun TranslationCompactLanguageName(language: String) {
    Text(
        text = language,
        modifier = Modifier.widthIn(max = COMPACT_LANGUAGE_MAXIMUM_WIDTH),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TranslationCompactMoreMenu(onChangeEngine: () -> Unit) {
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

private const val ANCHORED_RESULT_MAX_LINES = 5
private val COMPACT_LANGUAGE_MAXIMUM_WIDTH = 80.dp
private val COMPACT_SPEECH_LANGUAGE_PAIR_MAXIMUM_WIDTH = 220.dp
