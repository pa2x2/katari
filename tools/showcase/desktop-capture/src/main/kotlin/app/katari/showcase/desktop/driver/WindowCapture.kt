package app.katari.showcase.desktop.driver

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Stills of the app's windows, taken from the screen in device pixels. The private display has no window manager, so
 * a window on it is its content alone; the site draws the frame.
 */
internal class WindowCapture(private val captures: File, private val failures: File) {
    private val robot by lazy { Robot() }

    /**
     * Saves [window] as `[name].png` once two screenshots in a row are the same, with the pointer parked off it. The app
     * may still be animating or loading when the scene is done waiting for it.
     */
    fun capture(name: String, window: Window = Desktop.mainWindow()) {
        val still = stillScreenshot(window) ?: throw SceneFailure("The window never held still for $name")
        captures.mkdirs()
        ImageIO.write(still, "png", File(captures, "$name.png"))
    }

    /** Waits as [capture] does, for a click that must land on what [window] shows once it is done loading. */
    fun awaitStill(window: Window = Desktop.mainWindow()) {
        stillScreenshot(window) ?: throw SceneFailure("The window never held still")
    }

    private fun stillScreenshot(window: Window): BufferedImage? {
        Desktop.parkPointer()
        Desktop.settle()
        var previous: BufferedImage? = null
        repeat(STILL_ATTEMPTS) {
            val current = screenshot(window)
            if (previous != null && samePixels(current, previous)) return current
            previous = current
            Thread.sleep(STILL_INTERVAL_MS)
        }
        return null
    }

    /** Keeps what explains a failure apart from the captures: each window's screenshot and every node on screen. */
    fun saveFailureReport(name: String) {
        failures.mkdirs()
        val windows = Desktop.onAwt { Window.getWindows().filter { it.isShowing } }
        windows.forEachIndexed { index, window ->
            ImageIO.write(screenshot(window), "png", File(failures, "$name-window-$index.png"))
        }
        val nodes = Desktop.onAwt { Desktop.allNodes() }
        File(failures, "$name-nodes.txt").writeText(nodes.joinToString("\n"))
    }

    private fun screenshot(window: Window): BufferedImage {
        val bounds = Desktop.onAwt { Rectangle(window.locationOnScreen, window.size) }
        // The variant with the most pixels is the screen's own; the others are scaled to AWT's coordinates.
        val image = robot.createMultiResolutionScreenCapture(bounds).resolutionVariants.maxBy { it.getWidth(null) }
        return BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply { drawImage(image, 0, 0, null) }.dispose()
        }
    }

    private fun samePixels(a: BufferedImage, b: BufferedImage): Boolean =
        a.width == b.width &&
            a.height == b.height &&
            a.getRGB(0, 0, a.width, a.height, null, 0, a.width)
                .contentEquals(b.getRGB(0, 0, b.width, b.height, null, 0, b.width))

    private companion object {
        const val STILL_ATTEMPTS = 20
        const val STILL_INTERVAL_MS = 500L
    }
}
