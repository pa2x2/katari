package mihon.text.recognition.provider.onnx.session

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

/**
 * Loaded inference sessions shared by the provider's components, keyed by model file.
 *
 * Loading a model takes far longer than one inference, so sessions stay loaded once used until [releaseAll]. A
 * different file for the same slot (a new model revision) replaces the previous session. A replaced or released
 * session closes once the last inference leasing it has finished.
 */
internal class OnnxSessions {
    val environment: OrtEnvironment by lazy(OrtEnvironment::getEnvironment)
    private val sessions = mutableMapOf<String, LoadedSession>()

    /** The session for [file] in [slot], loaded if needed and kept open until the returned lease is closed. */
    @Synchronized
    fun lease(slot: String, file: File): Lease {
        val loaded = sessions[slot]?.takeIf { it.file == file }
            ?: load(file).also { sessions.put(slot, it)?.retire() }
        loaded.leases++
        return Lease(loaded)
    }

    /** Frees every loaded model; the next inference loads its model again. */
    @Synchronized
    fun releaseAll() {
        sessions.values.forEach(LoadedSession::retire)
        sessions.clear()
    }

    private fun load(file: File): LoadedSession {
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(INFERENCE_THREADS)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        return LoadedSession(file, environment.createSession(file.absolutePath, options))
    }

    inner class Lease internal constructor(private val loaded: LoadedSession) : AutoCloseable {
        val session: OrtSession get() = loaded.session
        private var closed = false

        override fun close() {
            synchronized(this@OnnxSessions) {
                if (closed) return
                closed = true
                loaded.leases--
                loaded.closeIfDone()
            }
        }
    }

    internal class LoadedSession(val file: File, val session: OrtSession) {
        var leases = 0
        private var retired = false

        fun retire() {
            retired = true
            closeIfDone()
        }

        fun closeIfDone() {
            if (retired && leases == 0) session.close()
        }
    }

    private companion object {
        /** Leaves cores free for decoding and rendering the page being read. */
        val INFERENCE_THREADS = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    }
}
