package mihon.desktop.security.pam

import com.sun.jna.Callback
import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

/**
 * The part of the system's Linux-PAM (`security/pam_appl.h`) that checks the user's sign-in. Each function is the C one
 * without its `pam_` prefix: [start] is `pam_start`.
 */
internal interface Pam : Library {
    fun start(service: String, user: String, conversation: PamConversationStruct, handle: PointerByReference): Int

    fun authenticate(handle: Pointer, flags: Int): Int

    fun end(handle: Pointer, status: Int): Int
}

/**
 * `struct pam_conv`: the function PAM asks the user through, the messages of one call at a time, and the pointer it
 * hands that function back, which the app doesn't use.
 */
@Structure.FieldOrder("conversation", "appData")
internal class PamConversationStruct(@JvmField var conversation: PamConversationFunction) : Structure() {
    @JvmField var appData: Pointer? = null
}

/**
 * `int (*conv)(int num_msg, const struct pam_message **msg, struct pam_response **resp, void *appdata_ptr)`. [messages]
 * points at [count] pointers to `struct pam_message`, as Linux-PAM passes them; the answers go in an array the function
 * allocates with `malloc` and stores at [responses], which PAM frees.
 */
internal fun interface PamConversationFunction : Callback {
    fun invoke(count: Int, messages: Pointer, responses: Pointer, appData: Pointer?): Int
}

internal const val PAM_SUCCESS = 0
internal const val PAM_PERM_DENIED = 6
internal const val PAM_AUTH_ERR = 7
internal const val PAM_MAXTRIES = 11
internal const val PAM_CONV_ERR = 19

/** `msg_style`s of `struct pam_message`. */
internal const val PAM_PROMPT_ECHO_OFF = 1
internal const val PAM_PROMPT_ECHO_ON = 2
internal const val PAM_ERROR_MSG = 3
internal const val PAM_TEXT_INFO = 4

/** The system's Linux-PAM, or null where none loads. */
internal val pam: Pam? by lazy {
    LIBRARY_NAMES.firstNotNullOfOrNull { name ->
        try {
            Native.load(name, Pam::class.java, LIBRARY_OPTIONS)
        } catch (_: UnsatisfiedLinkError) {
            null
        }
    }
}

// The versioned name first: distributions ship the unversioned one only with PAM's development files
private val LIBRARY_NAMES = listOf("libpam.so.0", "pam")

private val LIBRARY_OPTIONS = mapOf(
    Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method -> "pam_" + method.name },
)
