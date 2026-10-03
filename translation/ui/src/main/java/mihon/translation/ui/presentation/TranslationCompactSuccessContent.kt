package mihon.translation.ui.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.presentation.language.TranslationDirectionText
import mihon.translation.ui.presentation.language.TranslationLanguageArrow
import tachiyomi.i18n.*
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
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onChangeEngine: () -> Unit,
) {
    val showFullscreen = showExpand && !expanded
    val speechAvailable = sourceSpeechTarget != null && targetSpeechTarget != null && onSpeechToggle != null

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
            )
        }
        onDismiss?.let {
            Spacer(modifier = Modifier.width(8.dp))
            EdgeIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.action_close),
                onClick = it,
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    if (speechAvailable || showLanguageChange) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LanguagePill(
                language = sourceLanguage,
                speechTarget = sourceSpeechTarget,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
                onChooseLabel = stringResource(MR.strings.translation_change_source_language),
                onChoose = onChooseSource.takeIf { showLanguageChange },
                modifier = Modifier.weight(1f, fill = false),
            )
            TranslationLanguageArrow()
            LanguagePill(
                language = targetLanguage,
                speechTarget = targetSpeechTarget,
                speechState = speechState,
                onSpeechToggle = onSpeechToggle,
                onChooseLabel = stringResource(MR.strings.translation_change_target_language),
                onChoose = onChooseTarget.takeIf { showLanguageChange },
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    } else {
        TranslationDirectionText(
            text = languagePair,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FOOTER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Engines that require attribution name themselves; the rest still show which engine translated.
        val engineLabel = result.presentation.resultAttribution?.label ?: result.presentation.engineName
        Box(modifier = Modifier.weight(1f)) {
            EngineLabel(
                label = engineLabel,
                onChangeEngine = onChangeEngine.takeIf { showEngineChange },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFullscreen) {
                EdgeIconButton(
                    icon = Icons.Outlined.OpenInFull,
                    contentDescription = stringResource(MR.strings.action_expand),
                    onClick = onExpand,
                )
            }
            if (showCopy) {
                EdgeIconButton(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(MR.strings.copy),
                    onClick = { onCopy(result.translatedText) },
                )
            }
        }
    }
}

/**
 * A language with its own speech and picker: the speaker reads that side aloud, the name opens that side's picker.
 * Either half is left out when the host doesn't offer it.
 */
@Composable
private fun LanguagePill(
    language: String,
    speechTarget: TranslationResultSpeechTarget?,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
    onChooseLabel: String,
    onChoose: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(PILL_HEIGHT),
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (speechTarget != null && onSpeechToggle != null) {
                SpeechSegment(speechTarget, speechState, onSpeechToggle)
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxHeight()
                    .then(
                        if (onChoose != null) {
                            Modifier.clickable(onClickLabel = onChooseLabel, role = Role.Button, onClick = onChoose)
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = 10.dp, end = if (onChoose != null) 4.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onChoose != null) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeechSegment(
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(SPEECH_SEGMENT_WIDTH)
            .clickable(role = Role.Button, onClick = { onSpeechToggle(target) }),
        contentAlignment = Alignment.Center,
    ) {
        TranslationSpeechIndicator(target, speechState, iconSize = 18.dp)
    }
}

@Composable
private fun EngineLabel(
    label: String,
    onChangeEngine: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .height(FOOTER_HEIGHT)
            .then(
                if (onChangeEngine != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(MR.strings.translation_choose_engine),
                        role = Role.Button,
                        onClick = onChangeEngine,
                    )
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onChangeEngine != null) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * An icon that lines up with the content edge while keeping a full-size touch target, which overflows into the
 * popup's padding.
 */
@Composable
private fun EdgeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(EDGE_ICON_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.requiredSize(48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private const val ANCHORED_RESULT_MAX_LINES = 5
private val PILL_HEIGHT = 36.dp
private val SPEECH_SEGMENT_WIDTH = 40.dp
private val FOOTER_HEIGHT = 40.dp
private val EDGE_ICON_SIZE = 24.dp
