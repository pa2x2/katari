package app.katari.sync.client.connection

/**
 * Where the device keeps the token its server gave it when it registered (D014): a device secret, so never in the
 * database, a backup or a synced record, but in the platform's store for secrets. Calls may block, such as on the
 * desktop's keyring, so they run off the main thread.
 */
interface DeviceTokenStore {
    /** False where the platform has nowhere safe to keep it, such as a desktop without a keyring. */
    val isAvailable: Boolean

    fun read(): String?

    /** Saves [token], or forgets the saved one when it is null. Throws when it can't. */
    fun write(token: String?)
}
