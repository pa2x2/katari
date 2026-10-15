package eu.kanade.presentation.more.settings.screen.duplicate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.delay
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

/** Adds or edits a title exclusion pattern, rejecting blank, catch-all and repeated patterns. */
@Composable
internal fun DuplicateTitleExclusionEditorDialog(
    title: String,
    confirmLabel: String,
    initialValue: String,
    existingPatterns: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pattern by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val validation = remember(pattern, existingPatterns) {
        validatePattern(pattern, existingPatterns)
    }
    val normalizedPattern = remember(pattern) { DuplicateTitleExclusions.normalizePattern(pattern) }
    val normalizedInitialValue = remember(initialValue) { DuplicateTitleExclusions.normalizePattern(initialValue) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(
            label = confirmLabel,
            onClick = { onConfirm(normalizedPattern) },
            enabled = validation.isValid && normalizedPattern != normalizedInitialValue,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        TextField(
            value = pattern,
            onValueChange = { pattern = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = stringResource(MR.strings.pref_duplicate_detection_title_exclusions_pattern),
            supportingText = stringResource(
                validation.message ?: MR.strings.pref_duplicate_detection_title_exclusions_pattern_help,
            ),
            isError = !validation.isValid,
        )
    }

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

private fun validatePattern(pattern: String, existingPatterns: List<String>): PatternValidation {
    val normalized = DuplicateTitleExclusions.normalizePattern(pattern)
    return when {
        normalized.isBlank() -> PatternValidation(
            false,
            MR.strings.pref_duplicate_detection_title_exclusions_error_blank,
        )
        DuplicateTitleExclusions.isCatchAllPattern(normalized) ->
            PatternValidation(false, MR.strings.pref_duplicate_detection_title_exclusions_error_catch_all)
        existingPatterns.any { it.equals(normalized, ignoreCase = true) } ->
            PatternValidation(false, MR.strings.pref_duplicate_detection_title_exclusions_error_exists)
        else -> PatternValidation(true, null)
    }
}

private data class PatternValidation(
    val isValid: Boolean,
    val message: StringResource?,
)
