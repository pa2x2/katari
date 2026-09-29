package eu.kanade.tachiyomi.network.localnetwork

import java.io.IOException

/** A connection was refused because the app lacks the local network permission; [message] explains it to the user. */
class LocalNetworkAccessDeniedException(message: String) : IOException(message)

/** The local network denial behind this failure, when a client wrapped the network error in its own exception. */
fun Throwable.localNetworkAccessDenial(): LocalNetworkAccessDeniedException? {
    return generateSequence(this, Throwable::cause).filterIsInstance<LocalNetworkAccessDeniedException>().firstOrNull()
}
