package mihon.text.recognition.provider.onnx.session

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

/**
 * Loaded inference sessions shared by the provider's components, keyed by model file.
 *
 * Loading a model takes far longer than one inference, so sessions stay loaded once used. A different file for the
 * same slot (a new model revision) replaces and closes the previous session.
 */
internal class OnnxSessions {
    val environment: OrtEnvironment by lazy(OrtEnvironment::getEnvironment)
    private val sessions = mutableMapOf<String, Pair<File, OrtSession>>()

    @Synchronized
    fun session(slot: String, file: File): OrtSession {
        sessions[slot]?.let { (loadedFile, session) ->
            if (loadedFile == file) return session
            session.close()
        }
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(INFERENCE_THREADS)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        return environment.createSession(file.absolutePath, options).also { session ->
            sessions[slot] = file to session
        }
    }

    private companion object {
        /** Leaves cores free for decoding and rendering the page being read. */
        val INFERENCE_THREADS = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    }
}
