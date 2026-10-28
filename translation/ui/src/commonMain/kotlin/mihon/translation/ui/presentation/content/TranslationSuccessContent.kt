package mihon.translation.ui.presentation.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.OpenInFull
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.PaneDivider
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.TranslationResultSpeechSide
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.TranslationSpeechSection
import mihon.translation.ui.presentation.TranslationSpeechSectionHeader
import mihon.translation.ui.presentation.language.TranslationDirectionText
import mihon.translation.ui.presentation.language.TranslationLanguageChipPair
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A finished translation: the expanded layout shows the original and the result with their speech and language
 * controls, the compact popup hands off to [TranslationCompactSuccessContent].
 */
@Composable
internal fun SuccessContent(
    result: TranslationResult,
    sourceText: String?,
    expanded: Boolean,
    compact: Boolean,
    showExpand: Boolean,
    showLanguageChange: Boolean,
    showEngineChange: Boolean,
    showCopy: Boolean,
    onDismiss: (() -> Unit)?,
    onCopy: (String) -> Unit,
    onExpand: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: ((TranslationResultSpeechTarget) -> Unit)?,
    showResultLanguage: Boolean,
) {
    val showOverflowAction = showExpand && !expanded
    val languagePair = stringResource(
        MR.strings.translation_language_pair,
        result.sourceLanguage.displayName(),
        result.targetLanguage.displayName(),
    )
    val chooseSource = { onExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage) }
    val chooseTarget = { onExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) }
    val changeEngine = {
        onExternalAction(TranslationSessionExternalAction.ChooseEngine)
    }
    val speechContent = sourceText?.takeIf { onSpeechToggle != null }?.let { exactSourceText ->
        TranslationResultSpeechContent(
            sourceText = exactSourceText,
            sourceTarget = TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Source,
                text = exactSourceText,
                language = result.sourceLanguage,
            ),
            targetTarget = TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Target,
                text = result.translatedText,
                language = result.targetLanguage,
            ),
            onToggle = requireNotNull(onSpeechToggle),
        )
    }
    if (!compact) {
        if (speechContent != null) {
            TranslationSpeechSection(
                title = stringResource(MR.strings.translation_original),
                language = result.sourceLanguage.displayName(),
                text = speechContent.sourceText,
                target = speechContent.sourceTarget,
                speechState = speechState,
                onSpeechToggle = speechContent.onToggle,
                onChooseLanguage = chooseSource.takeIf { showLanguageChange },
            )
            PaneDivider()
            TranslationSpeechSectionHeader(
                title = stringResource(MR.strings.translation_title),
                language = result.targetLanguage.displayName(),
                target = speechContent.targetTarget,
                speechState = speechState,
                onSpeechToggle = speechContent.onToggle,
                onChooseLanguage = chooseTarget.takeIf { showLanguageChange },
            )
        } else if (showResultLanguage && showLanguageChange) {
            TranslationLanguageChipPair(
                sourceLanguage = result.sourceLanguage.displayName(),
                targetLanguage = result.targetLanguage.displayName(),
                onChooseSource = chooseSource,
                onChooseTarget = chooseTarget,
            )
        } else if (showResultLanguage) {
            TranslationDirectionText(
                text = languagePair,
                role = TextRole.Label,
                tone = ContentTone.Secondary,
            )
        }
    }
    if (compact) {
        TranslationCompactSuccessContent(
            result = result,
            languagePair = languagePair,
            sourceLanguage = result.sourceLanguage.displayName(),
            targetLanguage = result.targetLanguage.displayName(),
            sourceSpeechTarget = speechContent?.sourceTarget,
            targetSpeechTarget = speechContent?.targetTarget,
            speechState = speechState,
            onSpeechToggle = speechContent?.onToggle,
            expanded = expanded,
            showExpand = showExpand,
            showLanguageChange = showLanguageChange,
            showEngineChange = showEngineChange,
            showCopy = showCopy,
            onDismiss = onDismiss,
            onCopy = onCopy,
            onExpand = onExpand,
            onChooseSource = chooseSource,
            onChooseTarget = chooseTarget,
            onChangeEngine = changeEngine,
        )
    } else {
        SelectionContainer {
            Text(
                text = result.translatedText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                role = TextRole.ItemTitle,
            )
        }
        result.presentation.resultAttribution?.let { attribution ->
            Text(text = attribution.label, role = TextRole.Badge, tone = ContentTone.Secondary)
        }
        if (showCopy || showOverflowAction || showEngineChange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (showCopy) {
                    IconButton(
                        icon = MaterialSymbols.Rounded.ContentCopy,
                        contentDescription = stringResource(MR.strings.copy),
                        onClick = { onCopy(result.translatedText) },
                    )
                }
                if (showOverflowAction) {
                    IconButton(
                        icon = MaterialSymbols.Rounded.OpenInFull,
                        contentDescription = stringResource(MR.strings.action_expand),
                        onClick = onExpand,
                    )
                }
                if (showEngineChange) {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Settings,
                        contentDescription = stringResource(MR.strings.translation_choose_engine),
                        onClick = changeEngine,
                    )
                }
            }
        }
    }
}

private data class TranslationResultSpeechContent(
    val sourceText: String,
    val sourceTarget: TranslationResultSpeechTarget,
    val targetTarget: TranslationResultSpeechTarget,
    val onToggle: (TranslationResultSpeechTarget) -> Unit,
)
