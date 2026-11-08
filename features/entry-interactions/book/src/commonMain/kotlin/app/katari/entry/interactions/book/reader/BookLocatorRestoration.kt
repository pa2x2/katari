package app.katari.entry.interactions.book.reader

import app.katari.book.api.BookLocator
import app.katari.entry.interactions.book.preparation.PreparedBookPublication
import kotlinx.coroutines.CancellationException

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
