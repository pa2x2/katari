package app.katari.showcase.desktop.driver

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

/**
 * The desktop app under capture, as scenes drive it: through the screen and keyboard as a person would, with the
 * windows' accessibility trees standing in for eyes. Everything that reads AWT state runs on the AWT thread; the scene
 * itself runs on a thread of its own and waits.
 */
internal object Desktop {
    private val robot by lazy { Robot() }

    /** The app's main window, once it shows anything. */
    fun mainWindow(timeoutMs: Long = TIMEOUT_MS): Window = awaitValue("the app's main window", timeoutMs) {
        Window.getWindows().firstOrNull { it is ComposeWindow && it.isShowing && nodesOf(it).size > 1 }
    }

    /** A showing window of the app other than [mainWindow], such as picture-in-picture. */
    fun otherWindow(): Window {
        val main = mainWindow()
        return awaitValue("a second window") {
            Window.getWindows().firstOrNull { it is ComposeWindow && it.isShowing && it !== main }
        }
    }

    /** Gives [window] the size, in device pixels, that captures of it come out at, at the screen's top left. */
    fun resize(window: Window, width: Int, height: Int) = onAwt {
        val scale = window.graphicsConfiguration.defaultTransform.scaleX
        window.setBounds(0, 0, (width / scale).toInt(), (height / scale).toInt())
        window.validate()
    }

    fun find(selector: Selector, timeoutMs: Long = TIMEOUT_MS): Node =
        awaitValue(selector.toString(), timeoutMs) { allNodes().firstOrNull(selector::matches) }

    fun has(selector: Selector): Boolean = onAwt { allNodes().any(selector::matches) }

    fun awaitGone(selector: Selector, timeoutMs: Long = TIMEOUT_MS) {
        awaitValue("$selector to go", timeoutMs) { if (allNodes().none(selector::matches)) Unit else null }
    }

    /** Clicks the node [selector] finds once it is enabled, as a person waits for a button to become clickable. */
    fun click(selector: Selector, timeoutMs: Long = TIMEOUT_MS) = click(steadyBounds(selector.enabled(), timeoutMs))

    /** Clicks the point at the given fractions of [window]'s width and height. */
    fun clickAt(window: Window, x: Float, y: Float, button: Int = InputEvent.BUTTON1_DOWN_MASK) {
        val bounds = onAwt { Rectangle(window.locationOnScreen, window.size) }
        click(Point(bounds.x + (bounds.width * x).toInt(), bounds.y + (bounds.height * y).toInt()), button)
    }

    fun rightClickAt(window: Window, x: Float, y: Float) = clickAt(window, x, y, InputEvent.BUTTON3_DOWN_MASK)

    /**
     * Clicks the point at the given fractions of the main window until [shown] is on screen, as for a reader's menus,
     * which a click shows and the next hides: one click may land before the screen takes clicks.
     */
    fun clickToShow(x: Float, y: Float, shown: Selector) = clickUntil(x, y, "$shown to show") { has(shown) }

    /** As [clickToShow], until [shown] is gone. */
    fun clickToHide(x: Float, y: Float, shown: Selector) = clickUntil(x, y, "$shown to hide") { !has(shown) }

    private fun clickUntil(x: Float, y: Float, what: String, done: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!done()) {
            if (System.currentTimeMillis() > deadline) throw SceneFailure("Clicking never got $what")
            clickAt(mainWindow(), x, y)
            Thread.sleep(TOGGLE_WAIT_MS)
        }
    }

    /** Presses [keys] together, such as Shift and Slash for `?`, and lets go of them in reverse. */
    fun press(vararg keys: Int) {
        keys.forEach(robot::keyPress)
        robot.delay(KEY_HOLD_MS)
        keys.reversed().forEach(robot::keyRelease)
        robot.waitForIdle()
    }

    fun pressEscape() = press(KeyEvent.VK_ESCAPE)

    /** Replaces the text of the field [selector] finds, as the field's own accessibility does for assistive tools. */
    fun enterText(selector: Selector, text: String) {
        val field = find(selector)
        onAwt {
            val editable = field.context.accessibleEditableText ?: throw SceneFailure("$selector takes no text")
            editable.setTextContents(text)
        }
    }

    /**
     * Moves the slider [selector] finds to [value], as an assistive tool sets a slider: exactly, where a drag lands on
     * whatever its pixel stands for.
     */
    fun setProgress(selector: Selector, value: Float) {
        val slider = find(selector)
        val done = onAwt {
            val action = slider.semantics?.getOrNull(SemanticsActions.SetProgress)
                ?: throw SceneFailure("$selector can't be set")
            action.action?.invoke(value) == true
        }
        if (!done) throw SceneFailure("$selector refused $value")
    }

    /**
     * Scrolls the list holding the node [selector] finds until that node is at the list's top, as an assistive tool
     * scrolls a list.
     */
    fun scrollToTop(selector: Selector) {
        val target = find(selector)
        val done = onAwt {
            val list = generateSequence(target.parent, Node::parent)
                .firstOrNull { it.semantics?.getOrNull(SemanticsActions.ScrollBy) != null }
                ?: throw SceneFailure("No list holds $selector")
            // Compose scrolls in the window's device pixels; the bounds are in AWT's coordinates.
            val scale = list.window.graphicsConfiguration.defaultTransform.scaleY
            val distance = ((target.bounds.y - list.bounds.y) * scale).toFloat()
            list.semantics?.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, distance) == true
        }
        if (!done) throw SceneFailure("The list holding $selector didn't scroll")
    }

    /** Waits for the app to handle what is queued, and for images loaded over the network to fade in. */
    fun settle(extraMs: Long = SETTLE_MS) {
        robot.waitForIdle()
        Thread.sleep(extraMs)
    }

    /**
     * Moves the pointer off every window, so no control under it shows its hover state or tooltip in a capture. The
     * screen is larger than the windows the scenes size, so its top right corner is free.
     */
    fun parkPointer() {
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
        robot.mouseMove(screen.x + screen.width - 1, screen.y)
        robot.waitForIdle()
    }

    /** Closes the app's windows as their close buttons would, which ends the app as a person closing it does. */
    fun closeApp() = onAwt {
        Window.getWindows().filter { it.isShowing }.forEach { window ->
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
        }
    }

    internal fun allNodes(): List<Node> = Window.getWindows().filter { it.isShowing }.flatMap(::nodesOf)

    private fun click(bounds: Rectangle, button: Int = InputEvent.BUTTON1_DOWN_MASK) =
        click(Point(bounds.centerX.toInt(), bounds.centerY.toInt()), button)

    private fun click(point: Point, button: Int) {
        robot.mouseMove(point.x, point.y)
        robot.delay(POINTER_SETTLE_MS)
        robot.mousePress(button)
        robot.delay(CLICK_HOLD_MS)
        robot.mouseRelease(button)
        robot.waitForIdle()
    }

    /**
     * Where the node [selector] finds comes to rest. Lists and sheets animate their rows into place, so a node found
     * the moment it appears may still be moving, and a click at where it was lands on nothing.
     */
    private fun steadyBounds(selector: Selector, timeoutMs: Long): Rectangle {
        var bounds = find(selector, timeoutMs).bounds
        var steadySamples = 0
        val deadline = System.currentTimeMillis() + STEADY_TIMEOUT_MS
        while (steadySamples < STEADY_SAMPLES && System.currentTimeMillis() < deadline) {
            Thread.sleep(STEADY_INTERVAL_MS)
            val next = find(selector).bounds
            steadySamples = if (next == bounds) steadySamples + 1 else 0
            bounds = next
        }
        return bounds
    }

    private fun <T : Any> awaitValue(what: String, timeoutMs: Long = TIMEOUT_MS, read: () -> T?): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            onAwt(read)?.let { return it }
            if (System.currentTimeMillis() > deadline) throw SceneFailure("Nothing on screen matches $what")
            Thread.sleep(POLL_MS)
        }
    }

    fun <T> onAwt(block: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return block()
        val result = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }

    private const val TIMEOUT_MS = 30_000L
    private const val POLL_MS = 200L
    private const val SETTLE_MS = 1_500L
    private const val POINTER_SETTLE_MS = 50
    private const val CLICK_HOLD_MS = 40
    private const val KEY_HOLD_MS = 40
    private const val STEADY_INTERVAL_MS = 100L
    private const val STEADY_SAMPLES = 2
    private const val STEADY_TIMEOUT_MS = 2_000L
    private const val TOGGLE_WAIT_MS = 1_000L
}

/** What a scene expected and didn't find; the runner reports it with a screenshot. */
internal class SceneFailure(message: String) : Exception(message)
