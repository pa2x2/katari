package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPageTouch
import app.katari.core.common.web.ExtensionPageTouch.Phase
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class PageTouchscreenTest {

    // Chromium refuses an end that still names a finger, and the end of all that follows lifts the fingers in the
    // wrong order
    @Test
    fun `the first of two fingers lifts by a move that leaves it out`() {
        val screen = PageTouchscreen()

        val events = listOf(
            screen.event(touch(0, Phase.PRESSED)),
            screen.event(touch(1, Phase.PRESSED)),
            screen.event(touch(0, Phase.RELEASED)),
            screen.event(touch(1, Phase.RELEASED)),
        )

        events.map { it.type() to it.fingers() } shouldBe listOf(
            "touchStart" to listOf(0),
            "touchStart" to listOf(0, 1),
            "touchMove" to listOf(1),
            "touchEnd" to emptyList(),
        )
    }

    private fun touch(pointerId: Int, phase: Phase) = ExtensionPageTouch(
        pointerId = pointerId,
        phase = phase,
        tool = ExtensionPageTouch.Tool.FINGER,
        x = 10f,
        y = 20f,
        radiusX = 0f,
        radiusY = 0f,
        rotation = 0f,
        pressure = 1f,
    )

    private fun JsonObject.type() = getValue("type").jsonPrimitive.content

    private fun JsonObject.fingers() = getValue("touchPoints").jsonArray.map {
        it.jsonObject.getValue("id").jsonPrimitive.int
    }
}
