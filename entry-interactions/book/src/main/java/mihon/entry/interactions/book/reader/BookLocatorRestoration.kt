package mihon.entry.interactions.book.reader

import kotlinx.coroutines.CancellationException
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.preparation.PreparedBookPublication

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
