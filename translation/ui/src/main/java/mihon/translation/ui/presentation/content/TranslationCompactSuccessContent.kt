package mihon.translation.ui.presentation.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.OpenInFull
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.VerticalDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.TranslationSpeechIndicator
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
    val spacing = KatariTheme.spacing
    val showFullscreen = showExpand && !expanded
    val speechAvailable = sourceSpeechTarget != null && targetSpeechTarget != null && onSpeechToggle != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = result.translatedText,
                role = TextRole.ItemTitle,
                maxLines = ANCHORED_RESULT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        onDismiss?.let {
            Spacer(modifier = Modifier.width(spacing.s))
            EdgeIconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.action_close),
                onClick = it,
            )
        }
    }

    Spacer(modifier = Modifier.height(spacing.xs))

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
            role = TextRole.Label,
            tone = ContentTone.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
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
            horizontalArrangement = Arrangement.spacedBy(spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFullscreen) {
                EdgeIconButton(
                    icon = MaterialSymbols.Rounded.OpenInFull,
                    contentDescription = stringResource(MR.strings.action_expand),
                    onClick = onExpand,
                )
            }
            if (showCopy) {
                EdgeIconButton(
                    icon = MaterialSymbols.Rounded.ContentCopy,
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
    val spacing = KatariTheme.spacing
    Surface(
        modifier = modifier,
        container = SurfaceContainer.None,
        shape = KatariTheme.shapes.small,
        outlined = true,
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (speechTarget != null && onSpeechToggle != null) {
                SpeechSegment(speechTarget, speechState, onSpeechToggle)
                VerticalDivider(modifier = Modifier.fillMaxHeight())
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
                    .padding(
                        start = spacing.m,
                        end = if (onChoose != null) spacing.xs else spacing.m,
                        top = spacing.s,
                        bottom = spacing.s,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language,
                    modifier = Modifier.weight(1f, fill = false),
                    role = TextRole.Label,
                    tone = ContentTone.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onChoose != null) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                        tone = ContentTone.Secondary,
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
            .clickable(role = Role.Button, onClick = { onSpeechToggle(target) })
            .padding(horizontal = KatariTheme.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        TranslationSpeechIndicator(target, speechState, iconSize = KatariTheme.sizes.iconSmall)
    }
}

@Composable
private fun EngineLabel(
    label: String,
    onChangeEngine: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
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
            )
            .padding(vertical = KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            role = TextRole.Supporting,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onChangeEngine != null) {
            Icon(
                imageVector = MaterialSymbols.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                tone = ContentTone.Secondary,
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
        modifier = Modifier.size(KatariTheme.sizes.icon),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = Modifier.requiredSize(KatariTheme.sizes.touchTarget),
        )
    }
}

private const val ANCHORED_RESULT_MAX_LINES = 5
