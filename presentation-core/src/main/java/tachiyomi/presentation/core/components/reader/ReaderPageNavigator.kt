package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class ReaderPageNavigatorType {
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    ;

    fun isHorizontal() = this == HORIZONTAL_LTR || this == HORIZONTAL_RTL
}

@Composable
fun ReaderPageNavigator(
    type: ReaderPageNavigatorType,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onPageIndexChangeFinished: ((Int) -> Unit)? = null,
    showSinglePageLabel: Boolean = false,
    previousSectionDescription: String,
    nextSectionDescription: String,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val safeCurrentPage = currentPage.coerceIn(1, safeTotalPages)
    val state = remember(safeTotalPages) {
        SliderState(
            value = safeCurrentPage.toFloat(),
            steps = (safeTotalPages - 2).coerceAtLeast(0),
            valueRange = 1f..safeTotalPages.toFloat(),
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(safeCurrentPage, sliderDragged) {
        if (!sliderDragged) state.value = safeCurrentPage.toFloat()
    }
    state.onValueChange = {
        state.value = it
        onPageIndexChange(it.roundToInt() - 1)
    }
    state.onValueChangeFinished = {
        onPageIndexChangeFinished?.invoke(state.value.roundToInt() - 1)
    }
    LaunchedEffect(safeCurrentPage) {
        if (sliderDragged) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val mainAxisPadding = if (LocalConfiguration.current.smallestScreenWidthDp >= 720) 24.dp else 8.dp
    val backgroundColor = MaterialTheme.colorScheme
        .surfaceColorAtElevation(3.dp)
        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    val buttonColor = IconButtonDefaults.filledIconButtonColors(
        containerColor = backgroundColor,
        disabledContainerColor = backgroundColor,
    )

    if (type.isHorizontal()) {
        HorizontalReaderPageNavigator(
            isRtl = type == ReaderPageNavigatorType.HORIZONTAL_RTL,
            state = state,
            onNextSection = onNextSection,
            nextSectionEnabled = nextSectionEnabled,
            onPreviousSection = onPreviousSection,
            previousSectionEnabled = previousSectionEnabled,
            currentLabel = state.value.roundToInt().toString(),
            totalLabel = safeTotalPages.toString(),
            showSinglePageLabel = showSinglePageLabel,
            previousSectionDescription = previousSectionDescription,
            nextSectionDescription = nextSectionDescription,
            interactionSource = interactionSource,
            mainAxisPadding = mainAxisPadding,
            backgroundColor = backgroundColor,
            buttonColor = buttonColor,
            modifier = modifier,
        )
    } else {
        VerticalReaderPageNavigator(
            state = state,
            onNextSection = onNextSection,
            nextSectionEnabled = nextSectionEnabled,
            onPreviousSection = onPreviousSection,
            previousSectionEnabled = previousSectionEnabled,
            currentLabel = state.value.roundToInt().toString(),
            totalLabel = safeTotalPages.toString(),
            showSinglePageLabel = showSinglePageLabel,
            previousSectionDescription = previousSectionDescription,
            nextSectionDescription = nextSectionDescription,
            interactionSource = interactionSource,
            mainAxisPadding = mainAxisPadding,
            backgroundColor = backgroundColor,
            buttonColor = buttonColor,
            modifier = modifier,
        )
    }
}

@Composable
fun ReaderProgressNavigator(
    isRtl: Boolean,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentProgress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (Float) -> Unit,
    previousSectionDescription: String,
    nextSectionDescription: String,
    modifier: Modifier = Modifier,
) {
    val safeProgress = currentProgress.coerceIn(0f, 1f)
    val state = remember {
        SliderState(
            value = safeProgress,
            valueRange = 0f..1f,
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(safeProgress, sliderDragged) {
        if (!sliderDragged) state.value = safeProgress
    }
    state.onValueChange = {
        state.value = it
        onProgressChange(it)
    }
    state.onValueChangeFinished = { onProgressChangeFinished(state.value) }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.value) {
        if (sliderDragged) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val mainAxisPadding = if (LocalConfiguration.current.smallestScreenWidthDp >= 720) 24.dp else 8.dp
    val backgroundColor = MaterialTheme.colorScheme
        .surfaceColorAtElevation(3.dp)
        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
    val buttonColor = IconButtonDefaults.filledIconButtonColors(
        containerColor = backgroundColor,
        disabledContainerColor = backgroundColor,
    )
    val percentage = (state.value * 100).roundToInt()

    HorizontalReaderPageNavigator(
        isRtl = isRtl,
        state = state,
        onNextSection = onNextSection,
        nextSectionEnabled = nextSectionEnabled,
        onPreviousSection = onPreviousSection,
        previousSectionEnabled = previousSectionEnabled,
        currentLabel = "$percentage%",
        totalLabel = "100%",
        showSinglePageLabel = false,
        previousSectionDescription = previousSectionDescription,
        nextSectionDescription = nextSectionDescription,
        interactionSource = interactionSource,
        mainAxisPadding = mainAxisPadding,
        backgroundColor = backgroundColor,
        buttonColor = buttonColor,
        modifier = modifier,
    )
}

@Composable
private fun HorizontalReaderPageNavigator(
    isRtl: Boolean,
    state: SliderState,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentLabel: String,
    totalLabel: String,
    showSinglePageLabel: Boolean,
    previousSectionDescription: String,
    nextSectionDescription: String,
    interactionSource: MutableInteractionSource,
    mainAxisPadding: Dp,
    backgroundColor: Color,
    buttonColor: IconButtonColors,
    modifier: Modifier,
) {
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = mainAxisPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionButton(
                enabled = if (isRtl) nextSectionEnabled else previousSectionEnabled,
                onClick = if (isRtl) onNextSection else onPreviousSection,
                icon = Icons.Outlined.SkipPrevious,
                description = if (isRtl) nextSectionDescription else previousSectionDescription,
                colors = buttonColor,
            )

            if (state.valueRange.endInclusive > state.valueRange.start) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(backgroundColor)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(contentAlignment = Alignment.CenterEnd) {
                            Text(currentLabel)
                            Text(totalLabel, color = Color.Transparent)
                        }
                        Slider(
                            state = state,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            interactionSource = interactionSource,
                        )
                        Text(totalLabel)
                    }
                }
            } else if (showSinglePageLabel) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(currentLabel)
                    Slider(
                        state = remember { SliderState(value = 1f) },
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    Text(totalLabel)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            SectionButton(
                enabled = if (isRtl) previousSectionEnabled else nextSectionEnabled,
                onClick = if (isRtl) onPreviousSection else onNextSection,
                icon = Icons.Outlined.SkipNext,
                description = if (isRtl) previousSectionDescription else nextSectionDescription,
                colors = buttonColor,
            )
        }
    }
}

@Composable
private fun VerticalReaderPageNavigator(
    state: SliderState,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentLabel: String,
    totalLabel: String,
    showSinglePageLabel: Boolean,
    previousSectionDescription: String,
    nextSectionDescription: String,
    interactionSource: MutableInteractionSource,
    mainAxisPadding: Dp,
    backgroundColor: Color,
    buttonColor: IconButtonColors,
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
            icon = Icons.Outlined.SkipPrevious,
            description = previousSectionDescription,
            colors = buttonColor,
            modifier = Modifier.rotate(90f),
        )

        if (state.valueRange.endInclusive > state.valueRange.start) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(backgroundColor)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(currentLabel)
                VerticalSlider(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    interactionSource = interactionSource,
                )
                Text(totalLabel)
            }
        } else if (showSinglePageLabel) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Text("$currentLabel / $totalLabel")
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        SectionButton(
            enabled = nextSectionEnabled,
            onClick = onNextSection,
            icon = Icons.Outlined.SkipNext,
            description = nextSectionDescription,
            colors = buttonColor,
            modifier = Modifier.rotate(90f),
        )
    }
}

@Composable
private fun SectionButton(
    enabled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    colors: IconButtonColors,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        enabled = enabled,
        onClick = onClick,
        colors = colors,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = modifier,
        )
    }
}
