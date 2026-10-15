package mihon.entry.interactions.manga.media

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Slider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.m),
        container = SurfaceContainer.MediaScrim,
        shape = KatariTheme.shapes.full,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.CenterEnd) {
                Text(text = (targetPageIndex + 1).toString(), role = TextRole.Badge)
                // Not drawn: reserves the widest page number so the slider does not shift while scrubbing.
                Text(
                    text = pageCount.toString(),
                    modifier = Modifier.drawWithContent {},
                    role = TextRole.Badge,
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
                onMedia = true,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KatariTheme.spacing.s),
            )
            Text(text = pageCount.toString(), role = TextRole.Badge)
        }
    }
}
