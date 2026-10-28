package eu.kanade.presentation.util

import mihon.core.platform.PlatformContext

/**
 * Words an error that reaching a source raised, such as a failed refresh's, for the user: what an HTTP status, a lost
 * connection or a failing source script means rather than the exception's own text.
 */
interface SourceErrorMessages {
    fun message(context: PlatformContext, error: Throwable): String
}
