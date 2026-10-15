package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.presentation.kit.chip.AssistChip
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A translation's source language, which opens the source picker when tapped. */
@Composable
internal fun TranslationSourceLanguageChip(
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TranslationLanguageChip(
        language = language,
        onClickLabel = stringResource(MR.strings.translation_change_source_language),
        onClick = onClick,
        modifier = modifier,
    )
}

/** A translation's target language, which opens the target picker when tapped. */
@Composable
internal fun TranslationTargetLanguageChip(
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TranslationLanguageChip(
        language = language,
        onClickLabel = stringResource(MR.strings.translation_change_target_language),
        onClick = onClick,
        modifier = modifier,
    )
}

/** Both languages of a translation, each opening its own picker; long names shorten to keep both in view. */
@Composable
internal fun TranslationLanguageChipPair(
    sourceLanguage: String,
    targetLanguage: String,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TranslationSourceLanguageChip(sourceLanguage, onChooseSource, Modifier.weight(1f, fill = false))
        TranslationLanguageArrow()
        TranslationTargetLanguageChip(targetLanguage, onChooseTarget, Modifier.weight(1f, fill = false))
    }
}

/** The arrow between a translation's source and target languages. */
@Composable
internal fun TranslationLanguageArrow() {
    Icon(
        imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward,
        contentDescription = null,
        modifier = Modifier
            .padding(horizontal = KatariTheme.spacing.xs)
            .size(KatariTheme.sizes.iconSmall),
        tone = ContentTone.Secondary,
    )
}

@Composable
private fun TranslationLanguageChip(
    language: String,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    AssistChip(
        label = language,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = MaterialSymbols.Rounded.ArrowDropDown,
        onClickLabel = onClickLabel,
    )
}
