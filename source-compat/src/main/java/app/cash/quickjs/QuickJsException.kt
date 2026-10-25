package app.cash.quickjs

/**
 * Every error a [QuickJs] call raises, as in the binding Mihon and Aniyomi extensions were built against.
 *
 * @param jsStackTrace The JavaScript stack, also part of the message; null for errors that didn't come from a script.
 */
class QuickJsException @JvmOverloads constructor(
    message: String?,
    val jsStackTrace: String? = null,
) : RuntimeException(message)
