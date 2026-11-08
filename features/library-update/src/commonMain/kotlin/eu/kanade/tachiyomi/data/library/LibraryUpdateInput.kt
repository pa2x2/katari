package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * What one library update checks, as its work chain keeps it until the update runs.
 *
 * @property automatic queued by the profile's schedule rather than by the user.
 * @property categoryId with [sourceId] and [entryType], limits an update that follows the rules to one page.
 * @property selectionName the [LibraryUpdateSelectionFile] listing the entries to check whatever the rules say.
 * @property skippedOfLatest checks the entries the latest update skipped by rules.
 */
data class LibraryUpdateInput(
    val profileId: Long,
    val automatic: Boolean = false,
    val categoryId: Long? = null,
    val sourceId: Long? = null,
    val entryType: EntryType? = null,
    val selectionName: String? = null,
    val skippedOfLatest: Boolean = false,
)
