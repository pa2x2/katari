package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.translation.ui.presentation.language.TranslationSourceLanguageChip
import mihon.translation.ui.presentation.language.TranslationTargetLanguageChip
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationSpeechSection(
    title: String,
    language: String,
    text: String,
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    onChooseLanguage: (() -> Unit)? = null,
) {
    TranslationSpeechSectionHeader(
        title = title,
        language = language,
        target = target,
        speechState = speechState,
        onSpeechToggle = onSpeechToggle,
        onChooseLanguage = onChooseLanguage,
    )
    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun TranslationSpeechSectionHeader(
    title: String,
    language: String,
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
    onChooseLanguage: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            when {
                onChooseLanguage == null -> Text(
                    text = language,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                target.side == TranslationResultSpeechSide.Source ->
                    TranslationSourceLanguageChip(language, onChooseLanguage)
                else -> TranslationTargetLanguageChip(language, onChooseLanguage)
            }
        }
        TranslationSpeechActionButton(
            target = target,
            speechState = speechState,
            onSpeechToggle = onSpeechToggle,
        )
    }
}

@Composable
fun TranslationSpeechActionButton(
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
) {
    IconButton(
        onClick = { onSpeechToggle(target) },
        modifier = Modifier.size(48.dp),
    ) {
        TranslationSpeechIndicator(target, speechState, iconSize = 24.dp)
    }
}

/** Whether [target] can be played, is being prepared, or can be stopped, described for accessibility. */
@Composable
internal fun TranslationSpeechIndicator(
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    iconSize: Dp,
) {
    val active = speechState.activeTarget == target
    val contentDescription = stringResource(
        when (target.side) {
            TranslationResultSpeechSide.Source -> if (active) {
                MR.strings.translation_stop_original
            } else {
                MR.strings.translation_listen_original
            }
            TranslationResultSpeechSide.Target -> if (active) {
                MR.strings.translation_stop_result
            } else {
                MR.strings.translation_listen_result
            }
        },
    )
    if (active && speechState.phase == TranslationResultSpeechPhase.Preparing) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(iconSize - 2.dp)
                .semantics { this.contentDescription = contentDescription },
            strokeWidth = 2.dp,
        )
    } else {
        Icon(
            imageVector = if (active) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
