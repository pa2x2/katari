package app.katari.desktop.display.scroll.natural

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Whether the session's touchpads scroll naturally as the system sets them, from each look the display's readers take
 * at its devices ([on]). Under Xwayland that takes asking the compositor, which happens on a thread of its own, as its
 * answer can take a moment, and once for any number of looks while it runs.
 */
internal class SystemNaturalScrolling {
    private val state = MutableStateFlow<Boolean?>(null)

    /**
     * Null where it can't be read: no touchpad, touchpads set apart, or a compositor the app doesn't know how to ask.
     */
    val natural: StateFlow<Boolean?> = state.asStateFlow()

    private val compositorAsked = AtomicBoolean(false)
    private val asking = Executors.newSingleThreadExecutor { Thread(it, "Natural scrolling").apply { isDaemon = true } }

    fun on(touchpads: DisplayTouchpads) {
        when (touchpads) {
            is DisplayTouchpads.Server -> state.value = touchpads.natural
            DisplayTouchpads.Compositor -> if (compositorAsked.compareAndSet(false, true)) {
                asking.execute {
                    compositorAsked.set(false)
                    state.value = CompositorNaturalScrolling.read()
                }
            }
        }
    }
}
