package tachiyomi.domain.library.update.model

/**
 * A source whose entries library updates leave out.
 *
 * A pause whose end has passed no longer counts, so nothing has to run when it ends.
 *
 * @param until when the pause ends, in epoch milliseconds, or null when it lasts until the source is resumed.
 */
data class SourceUpdatePause(val sourceId: Long, val until: Long?) {

    fun isActiveAt(now: Long): Boolean = until == null || until > now

    companion object {
        // Stored in place of an end for a pause that lasts until the source is resumed.
        private const val UNTIL_RESUMED = 0L

        fun serialize(pause: SourceUpdatePause): String = "${pause.sourceId}:${pause.until ?: UNTIL_RESUMED}"

        fun deserialize(value: String): SourceUpdatePause? {
            val sourceId = value.substringBefore(':').toLongOrNull() ?: return null
            val until = value.substringAfter(':', "").toLongOrNull() ?: return null
            return SourceUpdatePause(sourceId, until.takeIf { it != UNTIL_RESUMED })
        }
    }
}
