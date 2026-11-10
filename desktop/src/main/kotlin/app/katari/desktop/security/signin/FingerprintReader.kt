package app.katari.desktop.security.signin

import kotlinx.coroutines.flow.Flow

/**
 * A fingerprint reader the user can sign in with instead of typing the password, read beside the system's sign-in
 * check where that check doesn't read it itself.
 */
interface FingerprintReader {
    /**
     * Waits for an enrolled finger while collected, which keeps the reader from other apps, and ends after
     * [FingerprintEvent.Recognized]. Ends without one where the reader can't be used, such as with no finger enrolled,
     * while another app reads it, or after the reader fails.
     */
    fun read(): Flow<FingerprintEvent>
}

enum class FingerprintEvent {
    /** The reader waits for a finger. */
    Waiting,

    /** A finger that matches none enrolled; the reader waits for another try. */
    NotRecognized,

    /** The reader couldn't take the finger in, such as one lifted too soon, and waits for it again. */
    ReadAgain,

    Recognized,
}
