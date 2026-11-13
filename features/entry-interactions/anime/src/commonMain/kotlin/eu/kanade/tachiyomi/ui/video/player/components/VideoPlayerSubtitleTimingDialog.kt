package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.TextField
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.parseSubtitleTimingSeconds
import eu.kanade.tachiyomi.ui.video.player.subtitle.timing.typedSubtitleTimingSeconds
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The subtitle timing typed in seconds, from the timing bar's value (D003), for a large offset known beforehand. It
 * opens with the timing selected, so what is typed replaces it.
 */
@Composable
internal fun VideoPlayerSubtitleTimingDialog(
    delayMs: Long,
    onSet: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val shown = typedSubtitleTimingSeconds(delayMs)
        mutableStateOf(TextFieldValue(text = shown, selection = TextRange(0, shown.length)))
    }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    val input = field.text
    val typed = parseSubtitleTimingSeconds(input)
    val set: () -> Unit = { typed?.let(onSet) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.anime_subtitle_timing_type),
        confirm = DialogAction(label = stringResource(MR.strings.action_ok), onClick = set, enabled = typed != null),
        dismiss = DialogAction(label = stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        TextField(
            value = field,
            onValueChange = { field = it },
            label = stringResource(MR.strings.anime_subtitle_timing),
            suffix = stringResource(MR.strings.anime_subtitle_timing_seconds_unit),
            supportingText = stringResource(
                if (input.isNotBlank() && typed == null) {
                    MR.strings.anime_subtitle_timing_invalid
                } else {
                    MR.strings.anime_subtitle_timing_type_hint
                },
            ),
            isError = input.isNotBlank() && typed == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { set() }),
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}
