package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.pointer.HiddenPointer
import app.katari.presentation.kit.pointer.LocalAppPointerPresence
import app.katari.presentation.kit.pointer.PointerMoves
import kotlinx.coroutines.delay

/**
 * Whether the immersive view's controls show. On a touch screen a tap on the media shows or hides them. Where the
 * window has a mouse, they follow it as the player's do: a move ([PointerMoves]) shows them, they hide
 * [CONTROLS_REST_MS] after the mouse rests unless it rests on a control (marked with `mediaControl()` against [hover]),
 * and at once when it leaves the app, and the pointer hides with them. A click on the media is then the media's own,
 * such as playing a clip.
 */
@Stable
internal class EntryImmersiveControls(val followsPointer: Boolean) {
    var visible by mutableStateOf(false)
        private set

    /** Hovers over the controls, which keep them shown while a mouse rests on one. */
    val hover = MutableInteractionSource()

    /** Observable: the mouse's moves so far, which restart the rest that hides the controls. */
    var moves by mutableLongStateOf(0L)
        private set
    var resting by mutableStateOf(true)
        private set

    val pointerIcon: PointerIcon
        get() = if (followsPointer && !visible && resting) HiddenPointer else PointerIcon.Default

    /** A tap on the media: shows or hides the controls on a touch screen, and leaves them to the mouse otherwise. */
    fun onMediaTap() {
        if (!followsPointer) visible = !visible
    }

    fun hide() {
        visible = false
    }

    fun pointerMoved() {
        moves += 1
        resting = false
        visible = true
    }

    suspend fun restAfterMoves(overControl: Boolean) {
        if (overControl) return
        delay(CONTROLS_REST_MS)
        resting = true
        visible = false
    }
}

@Composable
internal fun rememberEntryImmersiveControls(): EntryImmersiveControls {
    val presence = LocalAppPointerPresence.current
    val controls = remember(presence) { EntryImmersiveControls(followsPointer = presence != null) }
    if (presence != null) {
        val overControl by controls.hover.collectIsHoveredAsState()
        LaunchedEffect(controls, controls.moves, overControl) { controls.restAfterMoves(overControl) }
        LaunchedEffect(controls, presence) {
            snapshotFlow { presence.isOverApp }.collect { overApp -> if (!overApp) controls.hide() }
        }
    }
    return controls
}

/** Shows [controls] as the mouse moves over this, and hides the pointer with them while it rests. */
internal fun Modifier.immersiveControlsFollowPointer(controls: EntryImmersiveControls): Modifier {
    if (!controls.followsPointer) return this
    return pointerInput(controls) {
        awaitPointerEventScope {
            val moves = PointerMoves()
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Move && event.type != PointerEventType.Enter) continue
                val change = event.changes.firstOrNull() ?: continue
                if (change.type != PointerType.Mouse) continue
                if (!moves.isMove(change.position, controls.resting, density = this)) continue
                controls.pointerMoved()
            }
        }
    }.pointerHoverIcon(controls.pointerIcon)
}

private const val CONTROLS_REST_MS = 3_000L
