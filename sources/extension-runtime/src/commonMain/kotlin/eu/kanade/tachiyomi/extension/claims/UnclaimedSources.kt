package eu.kanade.tachiyomi.extension.claims

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import java.util.Collections
import java.util.IdentityHashMap

/**
 * The sources [before] held that none of [after] holds any more, so nothing calls them again and what they keep open
 * can be released. Compared by instance, not by ID: a reload hands an unchanged extension back with the instances it
 * already had, an update brings new instances under the same IDs, and an extension that lost a conflict still holds
 * its own.
 */
internal fun unclaimedSources(
    before: Collection<Extension.Loaded>,
    after: Collection<Extension.Loaded>,
): List<UnifiedSource> {
    val kept = Collections.newSetFromMap(IdentityHashMap<UnifiedSource, Boolean>())
    after.forEach { kept += it.sources }
    return before.flatMap { it.sources }.filterNot { it in kept }
}
