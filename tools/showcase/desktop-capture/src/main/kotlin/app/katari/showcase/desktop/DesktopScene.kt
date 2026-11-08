package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.WindowCapture
import java.awt.Window

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
}
