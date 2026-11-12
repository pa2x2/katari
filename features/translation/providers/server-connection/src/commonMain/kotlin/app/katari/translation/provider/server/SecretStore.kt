package app.katari.translation.provider.server

import app.katari.core.common.platform.PlatformFacility
import dev.icerock.moko.resources.StringResource

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

    /**
     * The key saved for [connection], or null when none is saved. Throws [java.io.IOException] when the store can't be
     * read now, such as a locked keyring, one that isn't running, or a Keystore that didn't answer: a later read may
     * get it, so it never stands for a key that isn't there.
     */
    fun read(connection: ServerConnectionId): String?

    /**
     * Lets the next [read] of [connection] ask the person to unlock the store again. Once they turned the question
     * down, or left it unanswered, reads don't ask any more and fail while the store stays locked, so work retried in
     * the background never puts it in front of them again; what they start themselves, such as syncing now, calls this
     * first. A store that never asks ignores it.
     */
    fun allowAsking(connection: ServerConnectionId)

    /** Saves [value] as the key of [connection], or removes the key when [value] is blank. Throws when it can't. */
    fun write(connection: ServerConnectionId, value: String)
}
