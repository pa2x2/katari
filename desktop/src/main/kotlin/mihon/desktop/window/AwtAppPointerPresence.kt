package mihon.desktop.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mihon.presentation.kit.pointer.AppPointerPresence
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * The pointer over the app's windows, from the enter and exit events AWT dispatches for every component in them. Compose
 * can't tell this itself: the pointer moving onto one of its pop-ups, such as a tooltip, leaves the window's content as
 * leaving the window does. Moving from one of the app's components into another exits the first before entering the
 * next, so an exit counts only once the events already queued have gone by without an enter.
 */
internal class AwtAppPointerPresence : AppPointerPresence, AutoCloseable {
    override var isOverApp by mutableStateOf(true)
        private set

    private var enters = 0L

    private val listener = AWTEventListener { event ->
        when (event.id) {
            MouseEvent.MOUSE_ENTERED -> {
                enters++
                isOverApp = true
            }
            MouseEvent.MOUSE_EXITED -> {
                val entersAtExit = enters
                SwingUtilities.invokeLater { if (enters == entersAtExit) isOverApp = false }
            }
        }
    }

    init {
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK)
    }

    override fun close() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
    }
}
