package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.dialog.DialogScrollEdges
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.ControlRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A value typed in under a list's entries, for one the entries don't offer, such as a playback speed between the
 * presets. [label] names the field, [text] puts a value in it and [parse] reads it back, null for text that isn't a
 * value; [invalid] says what to type instead.
 */
class ListTypedEntry<T>(
    val label: String,
    val suffix: String,
    val text: (T) -> String,
    val parse: (String) -> T?,
    val invalid: String,
)

/** With [typedEntry], the list's dialog also takes a typed value, set with OK; an entry picked still sets at once. */
@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    isProfileSpecific: Boolean,
    entries: Map<out T, String>,
    entryEnabled: (T) -> Boolean = { true },
    enabled: Boolean = true,
    typedEntry: ListTypedEntry<T>? = null,
    onValueChange: (T) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        isProfileSpecific = isProfileSpecific,
        enabled = enabled,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        var typed by remember { mutableStateOf(typedEntry?.text?.invoke(value).orEmpty()) }
        val typedValue = typedEntry?.parse?.invoke(typed)
        val setTyped = {
            if (typedValue != null) {
                onValueChange(typedValue)
                isDialogShown = false
            }
        }
        Dialog(
            onDismissRequest = { isDialogShown = false },
            title = title,
            confirm = typedEntry?.let {
                DialogAction(stringResource(MR.strings.action_ok), onClick = setTyped, enabled = typedValue != null)
            },
            dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = { isDialogShown = false }),
        ) {
            val state = rememberLazyListState()
            DialogScrollEdges(state) {
                RowList(state = state) {
                    entries.forEach { current ->
                        val isSelected = value == current.key
                        item {
                            ChoiceRow(
                                title = current.value,
                                selected = isSelected,
                                enabled = current.key?.let(entryEnabled) == true,
                                onClick = {
                                    if (!isSelected) {
                                        onValueChange(current.key!!)
                                        isDialogShown = false
                                    }
                                },
                            )
                        }
                    }
                    if (typedEntry != null) {
                        item {
                            ControlRow {
                                TextField(
                                    value = typed,
                                    onValueChange = { typed = it },
                                    label = typedEntry.label,
                                    suffix = typedEntry.suffix,
                                    supportingText = typedEntry.invalid.takeIf { typedValue == null },
                                    isError = typedValue == null,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { setTyped() }),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
