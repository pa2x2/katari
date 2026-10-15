package mihon.presentation.kit.control

import androidx.annotation.IntRange
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.KatariTheme
import kotlin.math.roundToInt

/**
 * A slider over whole numbers; by default every value in [valueRange] is a step. [onMedia] draws it in the media
 * content colours without step ticks, for scrubbers laid over pages or video.
 */
@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: IntProgression = 0..1,
    @IntRange(from = 0) steps: Int = with(valueRange) { ((last - first) - 1).coerceAtLeast(0) },
    onValueChangeFinished: (() -> Unit)? = null,
    onMedia: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val trackRange = with(valueRange) { first.toFloat()..last.toFloat() }
    val state = remember(steps, trackRange) {
        SliderState(value = value.toFloat(), steps = steps, trackRange = trackRange)
    }
    state.value = value.toFloat()
    androidx.compose.material3.Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = { onValueChange(it.roundToInt()) },
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors(onMedia),
        interactionSource = interactionSource,
    )
}

/** A continuous slider, such as a playback position; [steps] snaps it to that many evenly spaced values in between. */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onMedia: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = remember(steps, valueRange) { SliderState(value = value, steps = steps, trackRange = valueRange) }
    state.value = value
    androidx.compose.material3.Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors(onMedia),
        interactionSource = interactionSource,
    )
}

/** The continuous [Slider] laid out top to bottom, such as a page scrubber along the side of a reader. */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onMedia: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = remember(steps, valueRange) { SliderState(value = value, steps = steps, trackRange = valueRange) }
    state.value = value
    androidx.compose.material3.VerticalSlider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors(onMedia),
        interactionSource = interactionSource,
    )
}

@Composable
private fun sliderColors(onMedia: Boolean): SliderColors {
    if (!onMedia) return SliderDefaults.colors()
    val colors = KatariTheme.colors
    return SliderDefaults.colors(
        thumbColor = colors.onMedia,
        activeTrackColor = colors.onMedia,
        inactiveTrackColor = colors.onMediaVariant,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
        disabledThumbColor = colors.onMediaVariant,
        disabledActiveTrackColor = colors.onMediaVariant,
        disabledInactiveTrackColor = colors.onMediaVariant,
        disabledActiveTickColor = Color.Transparent,
        disabledInactiveTickColor = Color.Transparent,
    )
}
