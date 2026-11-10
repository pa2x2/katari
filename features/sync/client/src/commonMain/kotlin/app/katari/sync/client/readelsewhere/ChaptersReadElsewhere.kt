package app.katari.sync.client.readelsewhere

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Chapters of one entry that were unread here until sync took in another device's read. A chapter sync adds to the
 * list already read is not one: nothing on this device can belong to a chapter it never listed.
 */
data class ChaptersReadElsewhere(val entryId: Long, val chapterIds: Set<Long>)

/**
 * Told once the records that made chapters read here are committed, so what this device does after reading, such as
 * deleting downloads, follows reads on other devices too. A listener's failure is logged and never fails the sync.
 */
fun interface ChaptersReadElsewhereListener {
    suspend fun onReadElsewhere(reads: List<ChaptersReadElsewhere>)
}

/** Features that act on reads made elsewhere contribute their listeners; a graph may have none. */
@ContributesTo(AppScope::class)
interface ChaptersReadElsewhereBindings {
    @Multibinds(allowEmpty = true)
    fun readElsewhereListeners(): Set<ChaptersReadElsewhereListener>
}
