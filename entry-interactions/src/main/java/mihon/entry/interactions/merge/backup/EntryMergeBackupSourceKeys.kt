package mihon.entry.interactions.merge.backup

import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.persistence.backup.EntryBackupSourceKeys

/** A backed-up identity names its source by the key it had on the install the backup was made on. */
internal fun EntryMergeBackupIdentity.withLocalSourceKey(keys: EntryBackupSourceKeys) =
    copy(sourceId = keys.localKey(sourceId))
