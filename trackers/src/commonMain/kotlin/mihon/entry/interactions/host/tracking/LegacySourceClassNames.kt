package mihon.entry.interactions.host.tracking

import eu.kanade.tachiyomi.source.entry.UnifiedSource

/**
 * The class a Tachiyomi extension source was written as, which enhanced trackers such as Komga recognise their own
 * sources by. Null for every other source.
 */
fun interface LegacySourceClassNames {
    fun of(source: UnifiedSource): String?
}
