package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.icons.materialsymbols.rounded.Stop
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
        Text(text = text, role = TextRole.ItemTitle)
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
            Text(text = title, role = TextRole.SectionTitle)
            when {
                onChooseLanguage == null -> Text(text = language, role = TextRole.Supporting)
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

// An icon button whose icon turns into a loading indicator while the speech is prepared.
@Composable
fun TranslationSpeechActionButton(
    target: TranslationResultSpeechTarget,
    speechState: TranslationResultSpeechState,
    onSpeechToggle: (TranslationResultSpeechTarget) -> Unit,
) {
    Surface(
        onClick = { onSpeechToggle(target) },
        modifier = Modifier.size(KatariTheme.sizes.touchTarget),
        container = SurfaceContainer.None,
        shape = KatariTheme.shapes.full,
    ) {
        Box(contentAlignment = Alignment.Center) {
            TranslationSpeechIndicator(target, speechState, iconSize = KatariTheme.sizes.icon)
        }
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
        LoadingIndicator(
            modifier = Modifier
                .size(iconSize)
                .semantics { this.contentDescription = contentDescription },
        )
    } else {
        Icon(
            imageVector = if (active) MaterialSymbols.Rounded.Stop else MaterialSymbols.AutoMirroredRounded.VolumeUp,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tone = if (active) ContentTone.Primary else ContentTone.Secondary,
        )
    }
}
