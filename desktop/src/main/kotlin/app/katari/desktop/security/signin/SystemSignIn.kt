package app.katari.desktop.security.signin

import app.katari.desktop.application.thisProcessLaunch
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException

/**
 * The user's sign-in to this computer, checked by the system ([SystemSignInCheck]), as Android checks the device's
 * unlock, with a password or, where there is a [fingerprintReader], a finger. [isAvailable] is false where the system
 * can't check it.
 */
@SingleIn(AppScope::class)
@Inject
class SystemSignIn(private val check: SystemSignInCheck) {
    /** The account the check is for: the one running the app. */
    val account: String = System.getProperty("user.name")

    private val configuration: String? by lazy { check.configuration() }

    val isAvailable: Boolean
        get() = configuration != null

    /** The reader the user can sign in with instead of the password; null where [SystemSignInCheck] offers none. */
    val fingerprintReader: FingerprintReader? by lazy { configuration?.let(check::fingerprintReader) }

    /** Starts a check, in a process of its own (see [SignInCheckProcess]); null where none can start. */
    fun start(): SignInCheckRun? {
        val configuration = configuration ?: return null
        val launch = thisProcessLaunch() ?: return null
        return try {
            val process = ProcessBuilder(launch + SignInCheckProcess.argument(configuration))
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            SignInCheckRun(process)
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't start the sign-in check" }
            null
        }
    }
}

/** One check of the sign-in: what the system says and asks, and the answers to its questions. */
class SignInCheckRun internal constructor(private val process: Process) {
    private val answers = DataOutputStream(process.outputStream)

    /**
     * What the system says and asks, ending with [SignInCheckEvent.Finished]; a check that ends without telling how
     * it went finishes as [SignInOutcome.Failed]. Collected once; when the collection stops early, the check stops,
     * since reading the process's output blocks until it ends.
     */
    val events: Flow<SignInCheckEvent> = channelFlow {
        launch(Dispatchers.IO) {
            val input = DataInputStream(process.inputStream)
            while (true) {
                val event = try {
                    SignInCheckProtocol.readEvent(input)
                } catch (_: EOFException) {
                    SignInCheckEvent.Finished(SignInOutcome.Failed)
                } catch (e: IOException) {
                    logcat(LogPriority.WARN, e) { "The sign-in check stopped talking" }
                    SignInCheckEvent.Finished(SignInOutcome.Failed)
                }
                send(event)
                if (event is SignInCheckEvent.Finished) break
            }
            close()
        }
        awaitClose(::cancel)
    }

    /** Answers the question the system asked last. */
    fun answer(text: String) {
        try {
            SignInCheckProtocol.writeAnswer(answers, text)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "The sign-in check ended before its answer" }
        }
    }

    /** Stops the check without answering it, so the system counts no try. */
    private fun cancel() {
        process.destroyForcibly()
    }
}
