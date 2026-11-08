package mihon.presentation.kit.shortcut

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

/**
 * The keyboard shortcuts of one window, as declared by whatever is composed in it, and the keys the window hands over
 * to them through [onKeyEvent]. A platform without keyboard shortcuts provides none, and every declaration there does
 * nothing.
 *
 * Each overlay over the screen, such as a dialog, sheet or menu, opens a layer while it is shown. A shortcut of
 * [ShortcutReach.Screen] runs only from the top layer, so a screen's search doesn't open behind a dialog. When more
 * than one declaration of a key can run, the latest declared wins: a screen pushed or revealed is composed after the
 * one leaving, which may still be shown while the transition runs.
 */
@Stable
class KeyboardShortcutRegistry {
    internal val rootLayer = ShortcutLayer()
    private val layers = mutableListOf(rootLayer)
    private val registrations = mutableListOf<ShortcutRegistration>()
    private val typingFields = mutableSetOf<Any>()

    /** The shortcuts the list shows while it is open, as they were when it opened; null while it is closed. */
    internal var listed by mutableStateOf<List<KeyboardShortcut>?>(null)
        private set

    /** Hand the window's key events here once its content has had them, so a focused control keeps its own keys. */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        return trigger(event.toKeyPress())
    }

    /** Keys on their way to the focused control, for shortcuts taking them first ([KeyboardShortcut.beforeFocus]). */
    internal fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        return trigger(event.toKeyPress(), beforeFocusOnly = true)
    }

    /**
     * As [onKeyEvent], for keys that reach the window from elsewhere than as its Compose events, such as from the
     * platform or keys a web page left: [key] going down with these modifiers held. [typing] where the key was typed
     * into a field the window doesn't know of, such as a web page's.
     */
    fun onKeyDown(
        key: Key,
        ctrl: Boolean,
        alt: Boolean,
        shift: Boolean,
        meta: Boolean,
        typing: Boolean = false,
    ): Boolean = trigger(KeyPress(key = key, ctrl = ctrl, alt = alt, shift = shift, meta = meta), typing)

    /**
     * The mouse's back button, pressed anywhere in the window: runs the newest declaration that names
     * [ShortcutKeys.MouseBack] among its keys and can run, as a chord runs the newest declaration of its key. Returns
     * false when none can.
     */
    fun onMouseBack(): Boolean {
        val registration = registrations.lastOrNull { registration ->
            registration.canRun() && ShortcutKeys.MouseBack in registration.shortcut.keys
        } ?: return false
        registration.action()
        return true
    }

    internal fun trigger(
        press: KeyPress,
        typingElsewhere: Boolean = false,
        beforeFocusOnly: Boolean = false,
    ): Boolean {
        val typing = typingElsewhere || typingFields.isNotEmpty()
        val registration = registrations.lastOrNull { registration ->
            registration.canRun() &&
                (!typing || registration.shortcut.whileTyping) &&
                registration.shortcut.keys.any { it is ShortcutKeys.Chord && it.matches(press) }
        } ?: return false
        // The newest declaration of the key decides, so one that waits for focus keeps it from older ones that don't
        if (beforeFocusOnly && !registration.shortcut.beforeFocus) return false
        registration.action()
        return true
    }

    /** Lists what can run, leaving out a shortcut whose every key a newer declaration has taken, such as a screen's. */
    fun showList() {
        val runnable = registrations.filter { it.canRun() }
        listed = runnable
            .filterIndexed { index, registration ->
                val chords = registration.shortcut.keys.filterIsInstance<ShortcutKeys.Chord>()
                val taken = runnable.drop(index + 1).flatMap { newer ->
                    newer.shortcut.keys.filterIsInstance<ShortcutKeys.Chord>().map { it.press }
                }
                chords.isEmpty() || chords.any { it.press !in taken }
            }
            .asReversed()
            .distinctBy { it.shortcut.id }
            .asReversed()
            .map { it.shortcut }
    }

    internal fun hideList() {
        listed = null
    }

    internal fun register(registration: ShortcutRegistration) {
        registrations += registration
    }

    internal fun unregister(registration: ShortcutRegistration) {
        registrations -= registration
    }

    internal fun open(layer: ShortcutLayer) {
        layers += layer
    }

    internal fun close(layer: ShortcutLayer) {
        layers -= layer
    }

    internal fun typingStarted(field: Any) {
        typingFields += field
    }

    internal fun typingEnded(field: Any) {
        typingFields -= field
    }

    private fun ShortcutRegistration.canRun() =
        enabled && (shortcut.reach == ShortcutReach.Window || layer === layers.last())
}

/** Null where the platform has no keyboard shortcuts. */
val LocalKeyboardShortcuts = staticCompositionLocalOf<KeyboardShortcutRegistry?> { null }

internal class ShortcutLayer

/** One declaration of [shortcut], updated as the declaring composable recomposes. */
internal class ShortcutRegistration(
    val layer: ShortcutLayer,
    var shortcut: KeyboardShortcut,
    var enabled: Boolean,
    var action: () -> Unit,
)
