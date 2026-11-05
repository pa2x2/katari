package eu.kanade.tachiyomi.ui.reader.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import mihon.presentation.kit.pointer.HiddenPointer

/**
 * The mouse pointer over the pages, which goes once the mouse has rested a moment while [hidesWhenStill], as the
 * player hides it over the picture, and comes back on any move, press or scroll.
 */
@Composable
internal fun rememberReaderPointer(hidesWhenStill: Boolean): Modifier {
    var activity by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(hidesWhenStill, activity) {
        hidden = false
        if (!hidesWhenStill) return@LaunchedEffect
        delay(IDLE_MS)
        hidden = true
    }
    return Modifier
        .pointerInput(Unit) {
            var last = Offset.Unspecified
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val position = event.changes.firstOrNull()?.position ?: continue
                    // The window repeats the last move whenever what is under a still pointer is laid out anew
                    if (event.type == PointerEventType.Move && position == last) continue
                    last = position
                    activity++
                }
            }
        }
        .pointerHoverIcon(if (hidden) HiddenPointer else PointerIcon.Default)
}

private const val IDLE_MS = 2_000L
