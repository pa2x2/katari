package mihon.presentation.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.LocalTabbedSheetPagerScrollLock
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.style
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.showSoftKeyboard

/**
 * A value adjusted one meaningful step at a time, such as a text size. Tapping the value edits it as a number; while
 * editing, the tabbed sheet it sits in stops paging so a swipe over the keyboard cannot switch tabs.
 */
@Composable
fun Stepper(
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
    require(valueRange.first >= 0) { "Stepper only supports non-negative values" }
    require(step > 0) { "Stepper step must be positive" }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            label = "A−",
            contentDescription = decreaseContentDescription,
            enabled = value > valueRange.first,
            onClick = { onValueChange((value - step).coerceAtLeast(valueRange.first)) },
        )
        StepperValue(
            value = value,
            valueRange = valueRange,
            valueLabel = valueFormatter(value),
            inputSuffix = inputSuffix,
            editContentDescription = editContentDescription,
            onValueChange = onValueChange,
        )
        StepperButton(
            label = "A+",
            contentDescription = increaseContentDescription,
            enabled = value < valueRange.last,
            onClick = { onValueChange((value + step).coerceAtMost(valueRange.last)) },
        )
    }
}

@Composable
private fun StepperValue(
    value: Int,
    valueRange: IntRange,
    valueLabel: String,
    inputSuffix: String,
    editContentDescription: String,
    onValueChange: (Int) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    val pagerScrollLock = LocalTabbedSheetPagerScrollLock.current
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
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.extraLarge,
        onClickLabel = editContentDescription,
        modifier = Modifier
            .height(KatariTheme.sizes.touchTarget)
            .widthIn(min = ValueMinWidth),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m),
        ) {
            if (isEditing) {
                val initialText = value.toString()
                val state = remember(value) {
                    TextFieldState(
                        initialText = initialText,
                        initialSelection = TextRange(0, initialText.length),
                    )
                }
                val digitsOnly = InputTransformation {
                    if (length > valueRange.last.toString().length || asCharSequence().any { !it.isDigit() }) {
                        revertAllChanges()
                    }
                }
                val parsedValue = state.text.toString().toIntOrNull()
                val isValid = parsedValue != null && parsedValue in valueRange
                val finishEditing = {
                    if (isValid) onValueChange(requireNotNull(parsedValue))
                    stopEditing()
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        state = state,
                        inputTransformation = digitsOnly,
                        modifier = Modifier
                            .width(InputWidth)
                            .showSoftKeyboard(true)
                            .clearFocusOnSoftKeyboardHide(finishEditing),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        onKeyboardAction = {
                            if (isValid) finishEditing()
                        },
                        textStyle = TextRole.Body.style(MaterialTheme.typography).merge(
                            color = if (isValid) KatariTheme.colors.onSurface else KatariTheme.colors.error,
                            textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(KatariTheme.colors.primary),
                    )
                    Text(inputSuffix, role = TextRole.Body)
                }
            } else {
                Text(valueLabel, role = TextRole.Body)
            }
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.extraLarge,
        onClickLabel = contentDescription,
        modifier = Modifier.size(KatariTheme.sizes.touchTarget),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                role = TextRole.Label,
                tone = if (enabled) ContentTone.Primary else ContentTone.Disabled,
            )
        }
    }
}

private val ValueMinWidth = 76.dp
private val InputWidth = 40.dp
