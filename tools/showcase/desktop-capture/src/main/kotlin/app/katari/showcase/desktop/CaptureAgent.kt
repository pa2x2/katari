package app.katari.showcase.desktop

import java.io.File
import java.lang.instrument.Instrumentation

/**
 * Entry point of the agent the pipeline loads into the desktop app. Its argument is a properties file the pipeline
 * writes (see [AgentOptions]); the scene it names runs on a thread of its own once the app is up, as an instrumentation
 * runs beside an Android app.
 */
object CaptureAgent {
    @JvmStatic
    fun premain(arguments: String?, instrumentation: Instrumentation) {
        val file = File(requireNotNull(arguments) { "The capture agent needs its options file as its argument" })
        val options = AgentOptions.read(file)
        Thread({ SceneRunner(options).run() }, "showcase-scene").apply { isDaemon = true }.start()
    }
}
