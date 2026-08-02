package mihon.entry.interactions.manga.media

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.Slider

@Composable
internal fun MangaImmersivePageScrubber(
    currentPageIndex: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onScrubbingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return

    val safeCurrentPageIndex = currentPageIndex.coerceIn(0, pageCount - 1)
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isPressed || isDragging
    val latestOnPageSelected by rememberUpdatedState(onPageSelected)
    val latestOnScrubbingChange by rememberUpdatedState(onScrubbingChange)
    var targetPageIndex by remember(pageCount) { mutableIntStateOf(safeCurrentPageIndex) }
    var hasPendingSelection by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(safeCurrentPageIndex) {
        if (!isInteracting) targetPageIndex = safeCurrentPageIndex
    }
    LaunchedEffect(isInteracting) {
        latestOnScrubbingChange(isInteracting)
    }
    LaunchedEffect(targetPageIndex, isInteracting) {
        if (isInteracting) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    DisposableEffect(Unit) {
        onDispose { latestOnScrubbingChange(false) }
    }

    val contentColor = Color.White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(Color.Black.copy(alpha = 0.52f), CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                text = (targetPageIndex + 1).toString(),
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = pageCount.toString(),
                color = Color.Transparent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Slider(
            value = targetPageIndex,
            onValueChange = {
                targetPageIndex = it
                hasPendingSelection = true
            },
            valueRange = 0 until pageCount,
            onValueChangeFinished = {
                if (hasPendingSelection) latestOnPageSelected(targetPageIndex)
                hasPendingSelection = false
            },
            colors = SliderDefaults.colors(
                thumbColor = contentColor,
                activeTrackColor = contentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.32f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            interactionSource = interactionSource,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = pageCount.toString(),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
