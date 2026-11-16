package app.katari.presentation.kit.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Add
import app.katari.icons.materialsymbols.rounded.Remove
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.LocalRowDensity
import app.katari.presentation.kit.list.verticalPadding
import app.katari.presentation.kit.sheet.LocalTabbedSheetPagerScrollLock
import app.katari.presentation.kit.shortcut.typingFocus
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.theme.style
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.showSoftKeyboard

/** What a [Stepper] adjusts, which its buttons show. */
enum class StepperKind {
    /** A text size, or a size that scales text, such as the interface size: the buttons read A− and A+. */
    TextSize,

    /** Any other amount, such as a playback speed: the buttons are − and +. */
    Amount,
}

/**
 * How a [Stepper]'s value is typed: [text] puts the value in the field and [parse] reads the field back, null for text
 * that isn't a value. [decimal] lets the field take a decimal separator, for a value typed in fractions of its unit,
 * such as a speed of 115 typed as 1.15.
 */
class StepperInput(
    val text: (Int) -> String,
    val parse: (String) -> Int?,
    val decimal: Boolean,
) {
    companion object {
        val WholeNumber = StepperInput(text = Int::toString, parse = String::toIntOrNull, decimal = false)
    }
}

/**
 * A value adjusted one meaningful step at a time, such as a text size. Tapping the value edits it as [input] types it;
 * while editing, the tabbed sheet it sits in stops paging so a swipe over the keyboard cannot switch tabs, and the row
 * it sits in, buttons included, stays in view above the keyboard.
 */
@Composable
fun Stepper(
    value: Int,
    valueRange: IntRange,
    step: Int,
    kind: StepperKind,
    valueFormatter: (Int) -> String,
    inputSuffix: String,
    decreaseContentDescription: String,
    increaseContentDescription: String,
    editContentDescription: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    input: StepperInput = StepperInput.WholeNumber,
) {
    require(valueRange.first >= 0) { "Stepper only supports non-negative values" }
    require(step > 0) { "Stepper step must be positive" }

    val requester = remember { BringIntoViewRequester() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // Rows pad above and below their content by the row density's padding, so the stepper and that padding are its row
    val rowPadding = with(LocalDensity.current) { LocalRowDensity.current.verticalPadding.toPx() }
    val keepRowInView: suspend () -> Unit = {
        requester.bringIntoView(Rect(0f, -rowPadding, size.width.toFloat(), size.height + rowPadding))
    }
    Row(
        modifier = modifier
            .bringIntoViewRequester(requester)
            .onSizeChanged { size = it },
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            kind = kind,
            increases = false,
            contentDescription = decreaseContentDescription,
            enabled = value > valueRange.first,
            onClick = { onValueChange((value - step).coerceAtLeast(valueRange.first)) },
        )
        StepperValue(
            value = value,
            valueRange = valueRange,
            valueLabel = valueFormatter(value),
            input = input,
            inputSuffix = inputSuffix,
            editContentDescription = editContentDescription,
            keepRowInView = keepRowInView,
            onValueChange = onValueChange,
        )
        StepperButton(
            kind = kind,
            increases = true,
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
    input: StepperInput,
    inputSuffix: String,
    editContentDescription: String,
    keepRowInView: suspend () -> Unit,
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
                val initialText = input.text(value)
                val state = remember(value) {
                    TextFieldState(
                        initialText = initialText,
                        initialSelection = TextRange(0, initialText.length),
                    )
                }
                val typedOnly = InputTransformation {
                    val typed = asCharSequence()
                    val rejected = if (input.decimal) {
                        typed.any { !it.isDigit() && it !in DecimalSeparators } ||
                            typed.count { it in DecimalSeparators } > 1
                    } else {
                        length > valueRange.last.toString().length || typed.any { !it.isDigit() }
                    }
                    if (rejected) revertAllChanges()
                }
                // The field is one line tall, and a scrolling sheet or panel keeps only the focused field clear of the
                // keyboard as it opens, which would leave the rest of the row under it
                val ime = WindowInsets.ime
                val density = LocalDensity.current
                LaunchedEffect(Unit) {
                    snapshotFlow { ime.getBottom(density) }.collectLatest { keepRowInView() }
                }
                val parsedValue = input.parse(state.text.toString())
                val isValid = parsedValue != null && parsedValue in valueRange
                val finishEditing = {
                    if (isValid) onValueChange(requireNotNull(parsedValue))
                    stopEditing()
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        state = state,
                        inputTransformation = typedOnly,
                        modifier = Modifier
                            .width(InputWidth)
                            .typingFocus()
                            .showSoftKeyboard(true)
                            .clearFocusOnSoftKeyboardHide(finishEditing),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (input.decimal) KeyboardType.Decimal else KeyboardType.Number,
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
    kind: StepperKind,
    increases: Boolean,
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
            val tone = if (enabled) ContentTone.Primary else ContentTone.Disabled
            when (kind) {
                StepperKind.TextSize -> Text(text = if (increases) "A+" else "A−", role = TextRole.Label, tone = tone)
                StepperKind.Amount -> Icon(
                    imageVector = if (increases) MaterialSymbols.Rounded.Add else MaterialSymbols.Rounded.Remove,
                    contentDescription = contentDescription,
                    tone = tone,
                )
            }
        }
    }
}

private val ValueMinWidth = 76.dp
private val InputWidth = 40.dp
private val DecimalSeparators = charArrayOf('.', ',')
