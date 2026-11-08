package app.katari.entry.interactions.book.format.html.prosechapter.sanitization

import app.katari.entry.interactions.book.format.html.prosechapter.css.CssViewport
import java.awt.GraphicsEnvironment

// The default screen's bounds, which Java gives in scaled units, as a browser on that screen gives CSS pixels
internal actual fun screenViewport(): CssViewport? {
    if (GraphicsEnvironment.isHeadless()) return null
    return runCatching {
        val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
        CssViewport(bounds.width.toFloat(), bounds.height.toFloat())
    }.getOrNull()
}
