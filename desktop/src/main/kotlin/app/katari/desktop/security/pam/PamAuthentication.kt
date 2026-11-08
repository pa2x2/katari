package app.katari.desktop.security.pam

import app.katari.desktop.security.signin.SignInCheckEvent
import app.katari.desktop.security.signin.SignInConversation
import app.katari.desktop.security.signin.SignInOutcome
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.lang.ref.Reference

/**
 * Checks [user]'s sign-in through PAM's [service], with what the service says and asks going through [conversation].
 * Blocks until PAM answers: a module such as the fingerprint reader's waits on its own, between messages.
 */
internal fun authenticateWithPam(
    pam: Pam,
    service: String,
    user: String,
    conversation: SignInConversation,
): SignInOutcome {
    val function = PamConversationFunction { count, messages, responses, _ ->
        converse(count, messages, responses, conversation)
    }
    val handle = PointerByReference()
    val started = pam.start(service, user, PamConversationStruct(function), handle)
    if (started != PAM_SUCCESS) return SignInOutcome.Failed
    val status = pam.authenticate(handle.value, 0)
    pam.end(handle.value, status)
    // PAM copies the conversation struct but calls the function until it ends; JNA frees a callback's native stub
    // once its object is collected
    Reference.reachabilityFence(function)
    return when (status) {
        PAM_SUCCESS -> SignInOutcome.Accepted
        PAM_AUTH_ERR, PAM_MAXTRIES, PAM_PERM_DENIED -> SignInOutcome.Refused
        else -> SignInOutcome.Failed
    }
}

private fun converse(count: Int, messages: Pointer, responses: Pointer, conversation: SignInConversation): Int {
    val size = RESPONSE_SIZE * count
    val answers = Pointer(Native.malloc(size.toLong()))
    answers.clear(size.toLong())
    for (index in 0 until count) {
        val message = messages.getPointer(index.toLong() * Native.POINTER_SIZE)
        val style = message.getInt(0)
        val text = message.getPointer(Native.POINTER_SIZE.toLong())?.getString(0, Charsets.UTF_8.name()).orEmpty()
        val answer = when (style) {
            PAM_PROMPT_ECHO_OFF, PAM_PROMPT_ECHO_ON ->
                conversation.ask(SignInCheckEvent.Question(text, hidden = style == PAM_PROMPT_ECHO_OFF))
            PAM_ERROR_MSG, PAM_TEXT_INFO -> {
                conversation.tell(SignInCheckEvent.Message(text, isError = style == PAM_ERROR_MSG))
                null
            }
            // A binary prompt, which only special clients answer
            else -> return failConversation(answers, index)
        }
        if (answer != null) answers.setPointer(index.toLong() * RESPONSE_SIZE, nativeString(answer))
    }
    responses.setPointer(0, answers)
    return PAM_SUCCESS
}

/** Frees what the answers so far hold, since PAM frees the array only when the conversation succeeds. */
private fun failConversation(answers: Pointer, answered: Int): Int {
    for (index in 0 until answered) {
        answers.getPointer(index.toLong() * RESPONSE_SIZE)?.let { Native.free(Pointer.nativeValue(it)) }
    }
    Native.free(Pointer.nativeValue(answers))
    return PAM_CONV_ERR
}

/** [text] as a C string PAM can free. */
private fun nativeString(text: String): Pointer {
    val bytes = text.toByteArray(Charsets.UTF_8)
    val string = Pointer(Native.malloc(bytes.size + 1L))
    string.write(0, bytes, 0, bytes.size)
    string.setByte(bytes.size.toLong(), 0)
    return string
}

// `struct pam_response`: a pointer, then an int padded to the pointer's alignment
private val RESPONSE_SIZE = 2 * Native.POINTER_SIZE
