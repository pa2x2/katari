package eu.kanade.tachiyomi.source.failure

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * An error the runtime raised in an extension's code that the app's callers of a source, which catch exceptions, would
 * let through to the crash handler: one of those [isExtensionCodeError] names. [runExtensionCode] turns it into this
 * exception, so only that source's call fails, with the error shown, and the rest of the app goes on.
 *
 * It is unchecked because the proxy [containExtensionCode] puts around Entry SDK sources throws it from members that
 * declare no exceptions, where the JVM would wrap a checked one in an `UndeclaredThrowableException`.
 */
class ExtensionCodeException(error: Error) : RuntimeException(error.toString(), error) {
    /** The error's type, such as `VerifyError`, which says what went wrong where the message is pages of bytecode. */
    val errorType: String = error::class.simpleName ?: error::class.toString()
}

/**
 * Whether [error] is the extension's code failing in a way that leaves the app sound: a [LinkageError], for a class of
 * the extension's that can't be loaded, verified or initialised or a member it names that the app doesn't have, or a
 * [NotImplementedError], Kotlin's `TODO()` left in the extension. Other errors still end the app: running out of
 * memory or stack needn't be the extension's doing, and leaves the app nothing safe to go on with.
 */
fun isExtensionCodeError(error: Throwable): Boolean = error is LinkageError || error is NotImplementedError

/** Runs [block], which calls into an extension, with the errors [isExtensionCodeError] names turned into exceptions. */
inline fun <T> runExtensionCode(block: () -> T): T = try {
    block()
} catch (error: Error) {
    throw if (isExtensionCodeError(error)) extensionCodeFailure(error) else error
}

@PublishedApi
internal fun extensionCodeFailure(error: Error): ExtensionCodeException =
    ExtensionCodeException(error).also { it.logcat(LogPriority.ERROR, error) { "An extension's code failed" } }
