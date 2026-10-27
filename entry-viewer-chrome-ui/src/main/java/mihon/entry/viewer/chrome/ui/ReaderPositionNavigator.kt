package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SkipNext
import mihon.icons.materialsymbols.rounded.SkipPrevious
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Slider
import mihon.presentation.kit.control.VerticalSlider
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.theme.KatariTheme

/**
 * Shared reader seek control; callers define the position units and commit policy. [framed] draws the track and the
 * section buttons on pills of their own, for a navigator over the page; false leaves them to the bar around it, such as
 * a floating row of reader controls, where [leading] and [trailing] add that row's other actions beside the buttons.
 * [sliderModifier] reaches the slider, such as to follow where its thumb is.
 */
@Composable
fun ReaderPositionNavigator(
    type: ReaderPageNavigatorType,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    formatValue: (Float) -> String,
    endLabel: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    previousSectionDescription: String,
    nextSectionDescription: String,
    modifier: Modifier = Modifier,
    showSinglePageLabel: Boolean = false,
    onPositionClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    onValueChangeCancelled: (() -> Unit)? = null,
    framed: Boolean = true,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    sliderModifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var sliderValue by remember(valueRange, steps) { mutableFloatStateOf(value.coerceIn(valueRange)) }
    val currentOnCancel by rememberUpdatedState(onValueChangeCancelled)
    val interactionSource = remember { ReaderPositionInteractionSource(onCancel = { currentOnCancel?.invoke() }) }
    val sliderDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(value, sliderDragged) {
        if (!sliderDragged) sliderValue = value.coerceIn(valueRange)
    }
    val onSliderValueChange: (Float) -> Unit = {
        sliderValue = it
        onValueChange(it)
    }
    val onSliderValueChangeFinished: () -> Unit = {
        val cancelled = interactionSource.consumeCancellation()
        if (!cancelled || onValueChangeCancelled == null) onValueChangeFinished(sliderValue)
    }
    LaunchedEffect(value) {
        if (sliderDragged && hapticFeedbackEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val mainAxisPadding = if (!framed) {
        KatariTheme.sizes.none
    } else if (LocalConfiguration.current.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP) {
        KatariTheme.spacing.xl
    } else {
        KatariTheme.spacing.s
    }
    val track = PositionTrack(
        value = sliderValue,
        valueRange = valueRange,
        steps = steps,
        onValueChange = onSliderValueChange,
        onValueChangeFinished = onSliderValueChangeFinished,
        interactionSource = interactionSource,
    )
    if (type.isHorizontal()) {
        HorizontalReaderPageNavigator(
            isRtl = type == ReaderPageNavigatorType.HORIZONTAL_RTL,
            track = track,
            onNextSection = onNextSection,
            nextSectionEnabled = nextSectionEnabled,
            onPreviousSection = onPreviousSection,
            previousSectionEnabled = previousSectionEnabled,
            currentLabel = formatValue(sliderValue),
            totalLabel = endLabel,
            showSinglePageLabel = showSinglePageLabel,
            previousSectionDescription = previousSectionDescription,
            nextSectionDescription = nextSectionDescription,
            mainAxisPadding = mainAxisPadding,
            modifier = modifier,
            onPositionClick = onPositionClick,
            framed = framed,
            leading = leading,
            trailing = trailing,
            sliderModifier = sliderModifier,
        )
    } else {
        VerticalReaderPageNavigator(
            track = track,
            onNextSection = onNextSection,
            nextSectionEnabled = nextSectionEnabled,
            onPreviousSection = onPreviousSection,
            previousSectionEnabled = previousSectionEnabled,
            currentLabel = formatValue(sliderValue),
            totalLabel = endLabel,
            showSinglePageLabel = showSinglePageLabel,
            previousSectionDescription = previousSectionDescription,
            nextSectionDescription = nextSectionDescription,
            mainAxisPadding = mainAxisPadding,
            modifier = modifier,
        )
    }
}

private class PositionTrack(
    val value: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int,
    val onValueChange: (Float) -> Unit,
    val onValueChangeFinished: () -> Unit,
    val interactionSource: MutableInteractionSource,
) {
    val seekable: Boolean get() = valueRange.endInclusive > valueRange.start
}

@Composable
private fun HorizontalReaderPageNavigator(
    isRtl: Boolean,
    onPositionClick: (() -> Unit)?,
    track: PositionTrack,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentLabel: String,
    totalLabel: String,
    showSinglePageLabel: Boolean,
    previousSectionDescription: String,
    nextSectionDescription: String,
    mainAxisPadding: Dp,
    modifier: Modifier,
    framed: Boolean,
    leading: @Composable RowScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
    sliderModifier: Modifier,
) {
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    // The row runs left to right whatever the layout, so the row's start and end actions swap sides in a right-to-left one
    val outerDirection = LocalLayoutDirection.current
    val left = if (outerDirection == LayoutDirection.Ltr) leading else trailing
    val right = if (outerDirection == LayoutDirection.Ltr) trailing else leading
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = mainAxisPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides outerDirection) { left() }
            SectionButton(
                enabled = if (isRtl) nextSectionEnabled else previousSectionEnabled,
                onClick = if (isRtl) onNextSection else onPreviousSection,
                icon = MaterialSymbols.Rounded.SkipPrevious,
                description = if (isRtl) nextSectionDescription else previousSectionDescription,
                framed = framed,
            )

            if (track.seekable) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    NavigatorTrackSurface(modifier = Modifier.weight(1f), framed = framed) {
                        Row(
                            modifier = Modifier.padding(horizontal = KatariTheme.spacing.l),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = if (onPositionClick != null) {
                                    Modifier
                                        .sizeIn(
                                            minWidth = KatariTheme.sizes.touchTarget,
                                            minHeight = KatariTheme.sizes.touchTarget,
                                        )
                                        .clickable(onClick = onPositionClick)
                                } else {
                                    Modifier
                                },
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(currentLabel)
                                // Reserves the widest label's width so the track does not shift as the value changes.
                                Text(totalLabel, modifier = Modifier.alpha(0f).clearAndSetSemantics { })
                            }
                            Slider(
                                value = track.value,
                                onValueChange = track.onValueChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = KatariTheme.spacing.s)
                                    .then(sliderModifier),
                                valueRange = track.valueRange,
                                steps = track.steps,
                                onValueChangeFinished = track.onValueChangeFinished,
                                interactionSource = track.interactionSource,
                            )
                            Text(totalLabel)
                        }
                    }
                }
            } else if (showSinglePageLabel) {
                NavigatorTrackSurface(modifier = Modifier.weight(1f), framed = framed) {
                    Row(
                        modifier = Modifier.padding(horizontal = KatariTheme.spacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(currentLabel)
                        Slider(
                            value = 1f,
                            onValueChange = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = KatariTheme.spacing.s),
                        )
                        Text(totalLabel)
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            SectionButton(
                enabled = if (isRtl) previousSectionEnabled else nextSectionEnabled,
                onClick = if (isRtl) onPreviousSection else onNextSection,
                icon = MaterialSymbols.Rounded.SkipNext,
                description = if (isRtl) previousSectionDescription else nextSectionDescription,
                framed = framed,
            )
            CompositionLocalProvider(LocalLayoutDirection provides outerDirection) { right() }
        }
    }
}

@Composable
private fun VerticalReaderPageNavigator(
    track: PositionTrack,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentLabel: String,
    totalLabel: String,
    showSinglePageLabel: Boolean,
    previousSectionDescription: String,
    nextSectionDescription: String,
    mainAxisPadding: Dp,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = mainAxisPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionButton(
            enabled = previousSectionEnabled,
            onClick = onPreviousSection,
            icon = MaterialSymbols.Rounded.SkipPrevious,
            description = previousSectionDescription,
            modifier = Modifier.rotate(90f),
        )

        if (track.seekable) {
            NavigatorTrackSurface(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(vertical = KatariTheme.spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(currentLabel)
                    VerticalSlider(
                        value = track.value,
                        onValueChange = track.onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = KatariTheme.spacing.s),
                        valueRange = track.valueRange,
                        steps = track.steps,
                        onValueChangeFinished = track.onValueChangeFinished,
                        interactionSource = track.interactionSource,
                    )
                    Text(totalLabel)
                }
            }
        } else if (showSinglePageLabel) {
            NavigatorTrackSurface(modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$currentLabel / $totalLabel")
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        SectionButton(
            enabled = nextSectionEnabled,
            onClick = onNextSection,
            icon = MaterialSymbols.Rounded.SkipNext,
            description = nextSectionDescription,
            modifier = Modifier.rotate(90f),
        )
    }
}

@Composable
private fun NavigatorTrackSurface(
    modifier: Modifier,
    framed: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (framed) OverlayPill(modifier = modifier, content = content) else Box(modifier) { content() }
}

@Composable
private fun SectionButton(
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
) {
    val button = @Composable {
        IconButton(
            icon = icon,
            contentDescription = description,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }
    if (framed) OverlayPill { button() } else button()
}

private const val TABLET_SMALLEST_WIDTH_DP = 720
