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
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TranslationSpeechSection(
    title: String,
    language: String,
    text: String,
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
) {
    TranslationSpeechSectionHeader(
        title = title,
        language = language,
        target = target,
        speechState = speechState,
        onSpeechToggle = onSpeechToggle,
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = language,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    compact: Boolean = false,
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
    IconButton(
        onClick = { onSpeechToggle(target) },
        modifier = Modifier.size(48.dp),
    ) {
        if (active && speechState.phase == TranslationResultSpeechPhase.Preparing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(if (compact) 18.dp else 22.dp)
                    .semantics { this.contentDescription = contentDescription },
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = if (active) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                tint = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
