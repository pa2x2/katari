package tachiyomi.domain.source.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo

/** Names sources by what the source manager knows of them, leaving out the language while only one is enabled. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class EntryInfoSourceNames(
    private val sourceManager: SourceManager,
    private val preferences: SourcePreferences,
) : SourceInfoNames {
    override fun of(sourceId: Long): String =
        sourceManager.getDisplayInfo(sourceId).getDisplayNameForEntryInfo(preferences)
}
