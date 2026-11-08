package app.katari.sync.server.administration

/** How an admin's action on an account went; [Done] carries what the action produced. */
sealed interface AdminOutcome<out T> {
    data class Done<T>(val value: T) : AdminOutcome<T>

    /** The acting account is not an enabled admin (any longer); nothing changed. */
    data object NotAllowed : AdminOutcome<Nothing>

    data object NoSuchAccount : AdminOutcome<Nothing>

    /** The account is the last enabled admin, which must stay one so the server can still be managed. */
    data object LastAdmin : AdminOutcome<Nothing>

    data object UsernameTaken : AdminOutcome<Nothing>

    data object InvalidUsername : AdminOutcome<Nothing>

    data object InvalidPassword : AdminOutcome<Nothing>

    /** The username typed to confirm a deletion is not the account's. */
    data object NotConfirmed : AdminOutcome<Nothing>
}
