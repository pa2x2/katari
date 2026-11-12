package app.katari.sync.client.connection

/**
 * Where the device keeps the token its server gave it when it registered (D014): a device secret, so never in the
 * database, a backup or a synced record, but in the platform's store for secrets. Calls may block, such as on the
 * desktop's keyring, so they run off the main thread.
 */
interface DeviceTokenStore {
    /** False where the platform has nowhere safe to keep it, such as a desktop without a keyring. */
    val isAvailable: Boolean

    /**
     * The saved token, or null when none is saved. Throws [java.io.IOException] when the store can't be read now, such
     * as a locked keyring or one not started yet, which a later read may get past.
     */
    fun read(): String?

    /**
     * Lets the next [read] ask the person to unlock the store again, for a sync they started; reads stop asking once
     * the person turned the question down or left it unanswered.
     */
    fun allowAsking()

    /** Saves [token], or forgets the saved one when it is null. Throws when it can't. */
    fun write(token: String?)
}
