package mihon.translation.provider.server

import dev.icerock.moko.resources.StringResource
import mihon.core.common.platform.PlatformFacility

/**
 * The platform's store for the API keys of server connections, which the app never keeps in its own files: Android's
 * Keystore, or the desktop's keyring.
 */
@PlatformFacility
interface SecretStore {
    /**
     * False where the platform has no store to keep keys in, such as a desktop without a keyring; engines that
     * translate through a server are then not offered.
     */
    val isAvailable: Boolean

    /** What a server setup screen tells the user about where the key is kept. */
    val description: StringResource

    /** The key saved for [connection], or null when none is saved or the store can't be read. */
    fun read(connection: ServerConnectionId): String?

    /** Saves [value] as the key of [connection], or removes the key when [value] is blank. Throws when it can't. */
    fun write(connection: ServerConnectionId, value: String)
}
