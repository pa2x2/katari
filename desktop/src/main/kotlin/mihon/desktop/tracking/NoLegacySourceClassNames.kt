package mihon.desktop.tracking

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.entry.interactions.host.tracking.LegacySourceClassNames

/**
 * Desktop hosts no Tachiyomi extensions yet, only LNReader plugins, so no source has a legacy class for enhanced
 * trackers to recognise. Stands in until desktop loads APK extensions.
 */
@Inject
class NoLegacySourceClassNames : LegacySourceClassNames {
    override fun of(source: UnifiedSource): String? = null
}
