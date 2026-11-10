package app.katari.desktop.security.signin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where the fingerprint reader is in one sign-in dialog: what it said last while it reads, and the fingers it didn't
 * recognize. The count lasts the dialog, through the reads that stop while the app is in the background, so that
 * leaving and coming back doesn't give more tries; after [MAX_UNRECOGNIZED] only the password is left.
 */
internal class FingerprintPromptState {
    /** The reader's last word, or null while it doesn't read. */
    var status by mutableStateOf<FingerprintEvent?>(null)
        private set

    private var unrecognized by mutableIntStateOf(0)

    val tooManyTries: Boolean
        get() = unrecognized >= MAX_UNRECOGNIZED

    /** Takes [event] in; true once the read is over, with the finger recognized or too many tries. */
    fun take(event: FingerprintEvent): Boolean {
        if (event == FingerprintEvent.NotRecognized) unrecognized++
        status = event
        return event == FingerprintEvent.Recognized || tooManyTries
    }

    fun stoppedReading() {
        status = null
    }

    private companion object {
        // As many as pam_fprintd allows by default, which a desktop's lock screen reads the finger with
        const val MAX_UNRECOGNIZED = 3
    }
}
