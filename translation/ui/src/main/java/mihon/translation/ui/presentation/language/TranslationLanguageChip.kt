package mihon.translation.ui.presentation.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

/** Both languages of a translation, each opening its own picker. */
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
        TranslationSourceLanguageChip(sourceLanguage, onChooseSource)
        TranslationLanguageArrow()
        TranslationTargetLanguageChip(targetLanguage, onChooseTarget)
    }
}

/** The arrow between a translation's source and target languages. */
@Composable
internal fun TranslationLanguageArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
        contentDescription = null,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TranslationLanguageChip(
    language: String,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                .heightIn(min = CHIP_HEIGHT)
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language,
                modifier = Modifier.widthIn(max = CHIP_LABEL_MAXIMUM_WIDTH),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val CHIP_HEIGHT = 32.dp
private val CHIP_LABEL_MAXIMUM_WIDTH = 112.dp
