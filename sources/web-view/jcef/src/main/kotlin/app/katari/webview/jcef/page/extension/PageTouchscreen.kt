package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPageTouch
import app.katari.core.common.web.ExtensionPageTouch.Phase
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A page's touchscreen as the DevTools protocol takes it (`Input.dispatchTouchEvent`), which the page can't tell from
 * a person's. Touches go this way because those sent to a windowless browser itself never reach its page.
 *
 * The protocol is told of every finger still down with each event and works out which one changed, so the fingers
 * that are down are kept here. It lifts one finger of several by a move that leaves it out; an end that still names
 * fingers is refused. It knows no tool, so a stylus or mouse reaches the page as a finger.
 *
 * Coordinates are the page's CSS pixels, as hidden pages lay out at a scale of one. Not thread-safe: fed on the pages'
 * thread.
 */
internal class PageTouchscreen {
    private val down = LinkedHashMap<Int, JsonObject>()

    /** The parameters of the event that tells the page of [touch]. */
    fun event(touch: ExtensionPageTouch): JsonObject {
        val type = when (touch.phase) {
            Phase.PRESSED -> {
                down[touch.pointerId] = touch.point()
                "touchStart"
            }
            Phase.MOVED -> {
                down[touch.pointerId] = touch.point()
                "touchMove"
            }
            Phase.RELEASED -> {
                down.remove(touch.pointerId)
                if (down.isEmpty()) "touchEnd" else "touchMove"
            }
            Phase.CANCELLED -> {
                down.clear()
                "touchCancel"
            }
        }
        return buildJsonObject {
            put("type", type)
            put("touchPoints", JsonArray(down.values.toList()))
        }
    }

    private fun ExtensionPageTouch.point() = buildJsonObject {
        put("id", pointerId)
        put("x", x)
        put("y", y)
        put("radiusX", radiusX)
        put("radiusY", radiusY)
        put("rotationAngle", rotation)
        put("force", pressure)
    }
}
