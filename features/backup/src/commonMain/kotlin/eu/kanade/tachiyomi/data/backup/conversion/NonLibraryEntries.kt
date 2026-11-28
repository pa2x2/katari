package eu.kanade.tachiyomi.data.backup.conversion

import eu.kanade.tachiyomi.data.backup.models.BackupEntry

/**
 * Whether a converted entry is written into the Katari backup. Outside the library Katari keeps an entry only for its
 * progress, as its own backups do (D025), or for the merge it belongs to; any other would be gone after the next
 * backup. [hasProgress] is what the other app's entry holds of it.
 */
internal fun BackupEntry.isKept(hasProgress: Boolean): Boolean =
    favorite || hasProgress || mergeTargetUrl != null || mergedBySource
