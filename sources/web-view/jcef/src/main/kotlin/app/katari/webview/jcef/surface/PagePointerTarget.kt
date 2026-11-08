package app.katari.webview.jcef.surface

import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

/** The browser the mouse over a drawn page goes to, as the AWT events JCEF takes, at the page's CSS pixels. */
internal interface PagePointerTarget {
    /** Device pixels per CSS pixel the page is drawn at. */
    val scale: Double

    fun send(event: MouseEvent)

    fun send(event: MouseWheelEvent)
}
