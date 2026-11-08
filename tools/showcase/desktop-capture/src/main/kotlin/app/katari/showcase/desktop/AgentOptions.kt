package app.katari.showcase.desktop

import java.io.File
import java.util.Properties

/** What the pipeline asks of one launch of the app (tools/showcase/pipeline/desktop/desktop-app.ts writes it). */
internal class AgentOptions(
    /** The scene class's name under this package, such as `reader.BookScenes`. */
    val scene: String,
    val captureDirectory: File,
    val failureDirectory: File,
    /** Where the run's outcome goes: `OK`, or what failed. The pipeline waits for it. */
    val resultFile: File,
    /** Size in device pixels the main window is given, so captures come out this size. */
    val windowWidth: Int,
    val windowHeight: Int,
    /** Free-form arguments for the scene, such as a path or code. */
    val sceneArguments: Map<String, String>,
) {
    companion object {
        private const val SCENE_ARGUMENT_PREFIX = "argument."

        fun read(file: File): AgentOptions {
            val properties = Properties().apply { file.reader().use(::load) }
            fun value(key: String): String = properties.getProperty(key) ?: error("$file has no $key")
            return AgentOptions(
                scene = value("scene"),
                captureDirectory = File(value("captures")),
                failureDirectory = File(value("failures")),
                resultFile = File(value("result")),
                windowWidth = value("window.width").toInt(),
                windowHeight = value("window.height").toInt(),
                sceneArguments = properties.stringPropertyNames()
                    .filter { it.startsWith(SCENE_ARGUMENT_PREFIX) }
                    .associate { it.removePrefix(SCENE_ARGUMENT_PREFIX) to properties.getProperty(it) },
            )
        }
    }
}
