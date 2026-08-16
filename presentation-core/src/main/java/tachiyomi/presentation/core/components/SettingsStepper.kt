package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.Surface
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.showSoftKeyboard

/** App-styled discrete value control for settings that are adjusted one meaningful step at a time. */
@Composable
fun SettingsStepper(
    value: Int,
    valueRange: IntRange,
    step: Int,
    valueFormatter: (Int) -> String,
    inputSuffix: String,
    decreaseContentDescription: String,
    increaseContentDescription: String,
    editContentDescription: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(valueRange.first >= 0) { "Settings stepper only supports non-negative values" }
    require(step > 0) { "Settings stepper step must be positive" }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsStepperButton(
            label = "A−",
            contentDescription = decreaseContentDescription,
            enabled = value > valueRange.first,
            onClick = { onValueChange((value - step).coerceAtLeast(valueRange.first)) },
        )
        SettingsStepperValue(
            value = value,
            valueRange = valueRange,
            valueLabel = valueFormatter(value),
            inputSuffix = inputSuffix,
            editContentDescription = editContentDescription,
            onValueChange = onValueChange,
        )
        SettingsStepperButton(
            label = "A+",
            contentDescription = increaseContentDescription,
            enabled = value < valueRange.last,
            onClick = { onValueChange((value + step).coerceAtMost(valueRange.last)) },
        )
    }
}

/** Standard in-viewer settings row for a discrete app-styled stepper. */
@Composable
fun SettingsStepperItem(
    label: String,
    value: Int,
    valueRange: IntRange,
    step: Int,
    valueFormatter: (Int) -> String,
    inputSuffix: String,
    decreaseContentDescription: String,
    increaseContentDescription: String,
    editContentDescription: String,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        SettingsStepper(
            value = value,
            valueRange = valueRange,
            step = step,
            valueFormatter = valueFormatter,
            inputSuffix = inputSuffix,
            decreaseContentDescription = decreaseContentDescription,
            increaseContentDescription = increaseContentDescription,
            editContentDescription = editContentDescription,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun SettingsStepperValue(
    value: Int,
    valueRange: IntRange,
    valueLabel: String,
    inputSuffix: String,
    editContentDescription: String,
    onValueChange: (Int) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    val pagerScrollLock = LocalViewerSettingsPagerScrollLock.current
    val pagerScrollLockOwner = remember { Any() }

    DisposableEffect(pagerScrollLock, pagerScrollLockOwner) {
        onDispose { pagerScrollLock?.release(pagerScrollLockOwner) }
    }

    fun startEditing() {
        pagerScrollLock?.acquire(pagerScrollLockOwner)
        isEditing = true
    }

    fun stopEditing() {
        isEditing = false
        pagerScrollLock?.release(pagerScrollLockOwner)
    }

    Surface(
        onClick = ::startEditing,
        enabled = !isEditing,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onClickLabel = editContentDescription,
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 76.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            if (isEditing) {
                var input by remember(value) {
                    val text = value.toString()
                    mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
                }
                val parsedValue = input.text.toIntOrNull()
                val isValid = parsedValue != null && parsedValue in valueRange
                val finishEditing = {
                    if (isValid) onValueChange(requireNotNull(parsedValue))
                    stopEditing()
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = input,
                        onValueChange = { candidate ->
                            if (
                                candidate.text.length <= valueRange.last.toString().length &&
                                candidate.text.all(Char::isDigit)
                            ) {
                                input = candidate
                            }
                        },
                        modifier = Modifier
                            .width(40.dp)
                            .showSoftKeyboard(true)
                            .clearFocusOnSoftKeyboardHide(finishEditing),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValid) finishEditing()
                            },
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium + TextStyle(
                            color = if (isValid) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = inputSuffix,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsStepperButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.primary,
        onClickLabel = contentDescription,
        modifier = Modifier.size(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
