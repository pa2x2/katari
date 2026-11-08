package mihon.desktop.security.signin

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * How the app and its sign-in check process talk over the process's standard streams: the check sends each
 * [SignInCheckEvent] as it happens, and the app answers each [SignInCheckEvent.Question] with one answer.
 */
internal object SignInCheckProtocol {
    fun writeEvent(output: DataOutputStream, event: SignInCheckEvent) {
        when (event) {
            is SignInCheckEvent.Message -> {
                output.writeByte(MESSAGE)
                output.writeUTF(event.text)
                output.writeBoolean(event.isError)
            }
            is SignInCheckEvent.Question -> {
                output.writeByte(QUESTION)
                output.writeUTF(event.prompt)
                output.writeBoolean(event.hidden)
            }
            is SignInCheckEvent.Finished -> {
                output.writeByte(FINISHED)
                output.writeUTF(event.outcome.name)
            }
        }
        output.flush()
    }

    fun readEvent(input: DataInputStream): SignInCheckEvent = when (val tag = input.readByte().toInt()) {
        MESSAGE -> SignInCheckEvent.Message(input.readUTF(), input.readBoolean())
        QUESTION -> SignInCheckEvent.Question(input.readUTF(), input.readBoolean())
        FINISHED -> SignInCheckEvent.Finished(SignInOutcome.valueOf(input.readUTF()))
        else -> error("Unknown sign-in check event $tag")
    }

    fun writeAnswer(output: DataOutputStream, answer: String) {
        output.writeUTF(answer)
        output.flush()
    }

    fun readAnswer(input: DataInputStream): String = input.readUTF()

    private const val MESSAGE = 1
    private const val QUESTION = 2
    private const val FINISHED = 3
}
