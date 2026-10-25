package app.cash.quickjs

import eu.kanade.tachiyomi.source.javascript.EvaluationThreads
import eu.kanade.tachiyomi.source.javascript.toLegacyResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.system.logcat
import java.io.Closeable
import com.dokar.quickjs.QuickJs as Engine
import com.dokar.quickjs.QuickJsException as EngineException

/**
 * The QuickJS API Mihon and Aniyomi extensions link against by name (zhanghai/quickjs-java 547f5b1597, which
 * extensions-lib ships), run on quickjs-kt. Results keep that binding's shapes ([toLegacyResult]), and every call
 * runs on an [EvaluationThreads] thread, blocking the caller as the old binding did.
 */
class QuickJs private constructor(private val engine: Engine) : Closeable {

    fun evaluate(script: String): Any? = evaluate(script, "?")

    fun evaluate(script: String, fileName: String): Any? = runBlocking { evaluateSuspending(script, fileName) }

    fun compile(sourceCode: String, fileName: String): ByteArray = runBlocking {
        onEvaluationThread { engine.compile(sourceCode, fileName) }
    }

    fun execute(bytecode: ByteArray): Any? = runBlocking {
        onEvaluationThread { engine.evaluate<Any?>(bytecode).toLegacyResult() }
    }

    /** Exposing a Java object to scripts; no extension uses it. */
    fun <T> set(name: String, type: Class<T>, value: T): Unit =
        throw UnsupportedOperationException("QuickJs.set is not supported")

    /** Calling a script object through a Java interface; no extension uses it. */
    fun <T> get(name: String, type: Class<T>): T =
        throw UnsupportedOperationException("QuickJs.get is not supported")

    override fun close() {
        engine.close()
    }

    internal suspend fun evaluateSuspending(script: String, fileName: String = "?"): Any? =
        onEvaluationThread { engine.evaluate<Any?>(script, fileName).toLegacyResult() }

    private suspend fun <T> onEvaluationThread(block: suspend () -> T): T = withContext(EvaluationThreads.dispatcher) {
        try {
            block()
        } catch (e: EngineException) {
            throw QuickJsException(e.message, e.stack)
        }
    }

    companion object {
        @JvmStatic
        fun create(): QuickJs {
            val engine = Engine.create(EvaluationThreads.dispatcher).apply {
                maxStackSize = EvaluationThreads.MAX_STACK_SIZE
                // The old binding never ran Promise jobs, so scripts written for it may leave rejections unhandled
                unhandledRejectionHandler = { logcat(throwable = it) { "Unhandled promise rejection" } }
            }
            return QuickJs(engine)
        }
    }
}
