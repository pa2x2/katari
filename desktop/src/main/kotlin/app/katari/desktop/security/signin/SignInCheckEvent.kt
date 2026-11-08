package app.katari.desktop.security.signin

/** What the system says or asks while it checks the user's sign-in, in the order it does. */
sealed interface SignInCheckEvent {
    /**
     * Something to read in the system's own words, such as "Place your finger on the fingerprint reader" or why the
     * account is locked; [isError] for what the system marks as an error.
     */
    data class Message(val text: String, val isError: Boolean) : SignInCheckEvent

    /** An answer the system waits for, such as the password; [hidden] when what is typed shouldn't show. */
    data class Question(val prompt: String, val hidden: Boolean) : SignInCheckEvent

    data class Finished(val outcome: SignInOutcome) : SignInCheckEvent
}

enum class SignInOutcome {
    /** The system accepted the sign-in. */
    Accepted,

    /** The system turned the answers down, such as for a wrong password; the try counts toward its lockout. */
    Refused,

    /** The check couldn't run to an answer, such as when the system's sign-in service failed. */
    Failed,
}
