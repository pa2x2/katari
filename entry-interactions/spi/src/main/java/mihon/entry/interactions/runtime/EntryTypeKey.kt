package mihon.entry.interactions.runtime

import dev.zacsweers.metro.MapKey
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * Keys an Entry type module's provider into a `Map<EntryType, Provider>` multibinding. A type supports a capability
 * exactly when it contributes that capability's provider.
 */
@MapKey
annotation class EntryTypeKey(val value: EntryType)
