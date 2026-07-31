package mihon.entry.interactions.book

import kotlinx.coroutines.CancellationException
import mihon.book.api.BookLocator

/** Restores optional persisted position without making otherwise valid prepared content unavailable. */
internal suspend fun PreparedBookPublication.restoreLocator(locator: BookLocator?): BookLocator? {
    locator ?: return null
    if (validate(locator)) return locator
    val reconciled = try {
        reconcileMigratedLocator(locator)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }
    return reconciled?.takeIf(::validate)
}
