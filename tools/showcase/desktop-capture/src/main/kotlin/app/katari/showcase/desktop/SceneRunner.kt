package app.katari.showcase.desktop

import app.katari.showcase.desktop.driver.Desktop
import app.katari.showcase.desktop.driver.WindowCapture

/**
 * Runs the scene [options] names and writes how it went to the result file the pipeline waits for, then closes the
 * app, as the pipeline starts it afresh for every scene.
 */
internal class SceneRunner(private val options: AgentOptions) {
    fun run() {
        val stills = WindowCapture(options.captureDirectory, options.failureDirectory)
        val outcome = try {
            val scene = Class.forName("${javaClass.packageName}.${options.scene}")
                .getDeclaredConstructor()
                .newInstance() as DesktopScene
            val main = Desktop.mainWindow(timeoutMs = STARTUP_TIMEOUT_MS)
            Desktop.resize(main, options.windowWidth, options.windowHeight)
            Desktop.settle()
            scene.run(SceneContext(options, stills))
            OK
        } catch (failure: Throwable) {
            try {
                stills.saveFailureReport(options.scene)
            } catch (reportFailure: Exception) {
                failure.addSuppressed(reportFailure)
            }
            failure.stackTraceToString()
        }
        options.resultFile.writeText(outcome)
        Desktop.closeApp()
    }

    private companion object {
        const val OK = "OK"

        // A first start converts the extensions to jars and migrates the database before its window shows.
        const val STARTUP_TIMEOUT_MS = 3 * 60_000L
    }
}
