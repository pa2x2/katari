package app.katari.presentation.kit.navigation

import androidx.compose.animation.core.animate
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.presentation.kit.pointer.onWheelScrollEnd
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Has a large title follow a mouse wheel and a touchpad as it follows touch. Compose hands the bar's scroll behaviour
 * only the wheel steps the content can take and never flings after them, so the title would stay part-way collapsed
 * where a scroll stopped, and couldn't be brought back over content already at its top.
 *
 * A title a scroll left part-way goes on the way the scroll last went, as a touch fling takes it: open after a scroll
 * toward the top, collapsed after one away from it. A step toward the top that nothing scrolled by, over content
 * already there, opens it, as a drag past the top does.
 */
internal fun Modifier.appBarFollowsWheel(behavior: AppBarScrollBehavior): Modifier = this
    .onWheelScrollEnd { lastStep -> behavior.settle(open = lastStep.y > 0f) }
    .pointerInput(behavior) {
        coroutineScope {
            var opening: Job? = null
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    if (event.type != PointerEventType.Scroll) continue
                    val step = event.changes.firstOrNull() ?: continue
                    // A wheel turned toward the top is a step up, the opposite of nested scrolling's sign
                    if (step.isConsumed || step.scrollDelta.y >= 0f || opening?.isActive == true) continue
                    opening = launch { behavior.animateTo(open = true) }
                }
            }
        }
    }

private suspend fun AppBarScrollBehavior.settle(open: Boolean) {
    val offset = state.heightOffset
    if (offset == 0f || offset == state.heightOffsetLimit) return
    animateTo(open)
}

private suspend fun AppBarScrollBehavior.animateTo(open: Boolean) {
    val spec = snapAnimationSpec ?: return
    val target = if (open) 0f else state.heightOffsetLimit
    if (state.heightOffset == target) return
    animate(initialValue = state.heightOffset, targetValue = target, animationSpec = spec) { value, _ ->
        state.heightOffset = value
    }
}
