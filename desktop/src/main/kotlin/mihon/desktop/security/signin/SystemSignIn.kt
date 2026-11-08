package mihon.desktop.security.signin

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import logcat.LogPriority
import mihon.desktop.application.thisProcessLaunch
import mihon.desktop.security.pam.findPamService
import mihon.desktop.security.pam.pam
import tachiyomi.core.common.util.system.logcat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException

/**
 * The user's sign-in to this computer, checked by the system through PAM, as Android checks the device's unlock.
 * [isAvailable] is false where the system has no PAM or no sign-in service the app can use.
 */
@SingleIn(AppScope::class)
@Inject
class SystemSignIn {
    /** The account the check is for: the one running the app. */
    val account: String = System.getProperty("user.name")

    private val service: String? by lazy { if (pam == null) null else findPamService() }

    val isAvailable: Boolean
        get() = service != null

    /** Starts a check, in a process of its own (see [SignInCheckProcess]); null where none can start. */
    fun start(): SignInCheckRun? {
        val service = service ?: return null
        val launch = thisProcessLaunch() ?: return null
        return try {
            val process = ProcessBuilder(launch + SignInCheckProcess.argument(service))
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            SignInCheckRun(process)
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't start the sign-in check" }
            null
        }
    }
}

/** One check of the sign-in: what the system says and asks, the answers to its questions, and a way to stop it. */
class SignInCheckRun internal constructor(private val process: Process) {
    private val answers = DataOutputStream(process.outputStream)

    /**
     * What the system says and asks, ending with [SignInCheckEvent.Finished]; a check that ends without telling how
     * it went finishes as [SignInOutcome.Failed]. Collected once.
     */
    val events: Flow<SignInCheckEvent> = flow {
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
            emit(event)
            if (event is SignInCheckEvent.Finished) break
        }
    }.flowOn(Dispatchers.IO)

    /** Answers the question the system asked last. */
    fun answer(text: String) {
        try {
            SignInCheckProtocol.writeAnswer(answers, text)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "The sign-in check ended before its answer" }
        }
    }

    /** Stops the check without answering it, so the system counts no try. */
    fun cancel() {
        process.destroyForcibly()
    }
}
