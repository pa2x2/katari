package mihon.desktop.security.signin

import mihon.desktop.security.pam.PamConversation
import mihon.desktop.security.pam.authenticateWithPam
import mihon.desktop.security.pam.pam
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import kotlin.system.exitProcess

/**
 * The app started as a sign-in check: it checks the user's sign-in through one PAM service and tells the app what
 * the system says and asks, over [SignInCheckProtocol]. It runs as a process of its own because PAM has no way to stop
 * a check: answering a question with nothing or an error counts as a wrong try toward the system's lockout, and a
 * module such as the fingerprint reader's can't be interrupted at all. Ending this process stops the check without an
 * answer, so the app cancels a check by ending it, and the check ends itself the same way once the app stops
 * listening.
 */
internal object SignInCheckProcess {
    private const val PREFIX = "--sign-in-check="

    fun argument(service: String): String = PREFIX + service

    /** The PAM service [arguments] start a sign-in check with, or null for any other start. */
    fun serviceIn(arguments: Array<String>): String? =
        arguments.firstOrNull { it.startsWith(PREFIX) }?.removePrefix(PREFIX)

    fun run(service: String): Nothing {
        // Standard output carries the protocol, so whatever else prints goes to the error stream
        val output = DataOutputStream(FileOutputStream(FileDescriptor.out))
        System.setOut(System.err)
        val input = DataInputStream(System.`in`)
        val user = System.getProperty("user.name")
        val outcome = pam?.let { authenticateWithPam(it, service, user, StreamConversation(input, output)) }
            ?: SignInOutcome.Failed
        SignInCheckProtocol.writeEvent(output, SignInCheckEvent.Finished(outcome))
        exitProcess(0)
    }

    /** Ends the check the moment the app stops listening, without answering PAM, which would count a wrong try. */
    private class StreamConversation(
        private val input: DataInputStream,
        private val output: DataOutputStream,
    ) : PamConversation {
        override fun tell(message: SignInCheckEvent.Message) = whileAppListens {
            SignInCheckProtocol.writeEvent(output, message)
        }

        override fun ask(question: SignInCheckEvent.Question): String = whileAppListens {
            SignInCheckProtocol.writeEvent(output, question)
            SignInCheckProtocol.readAnswer(input)
        }

        private inline fun <T> whileAppListens(talk: () -> T): T = try {
            talk()
        } catch (_: IOException) {
            Runtime.getRuntime().halt(0)
            throw IllegalStateException("The process halted")
        }
    }
}
