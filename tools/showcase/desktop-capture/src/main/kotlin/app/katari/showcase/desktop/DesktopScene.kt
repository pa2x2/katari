package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.WindowCapture
import java.awt.Window
import java.io.File

/**
 * One launch of the app's worth of captures, named by its class under this package, as the pipeline's scenes.ts lists
 * it. The runner starts it once the main window shows, at the size captures are taken at.
 */
internal interface DesktopScene {
    fun run(scene: SceneContext)
}

internal class SceneContext(private val options: AgentOptions, private val stills: WindowCapture) {
    fun capture(name: String, window: Window = Desktop.mainWindow()) = stills.capture(name, window)

    fun awaitStill(window: Window = Desktop.mainWindow()) = stills.awaitStill(window)

    fun argument(name: String): String =
        options.sceneArguments[name] ?: error("The pipeline passed ${options.scene} no argument $name")

    /**
     * For a scene that runs alongside a phone scene: waits until the pipeline creates the file the `release` argument
     * names, once the phone scene is done with what this one does, so the app stays open while the phone needs it.
     */
    fun awaitRelease() {
        val release = File(argument("release"))
        val deadline = System.currentTimeMillis() + RELEASE_TIMEOUT_MS
        while (!release.exists()) {
            if (System.currentTimeMillis() > deadline) error("The pipeline never released ${options.scene}")
            Thread.sleep(RELEASE_POLL_MS)
        }
    }

    private companion object {
        // Longer than the phone scenes take to wait for what the desktop does.
        const val RELEASE_TIMEOUT_MS = 10 * 60_000L
        const val RELEASE_POLL_MS = 250L
    }
}
