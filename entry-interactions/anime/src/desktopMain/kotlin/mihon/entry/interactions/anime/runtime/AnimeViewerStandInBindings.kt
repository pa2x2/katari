package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.navigation.EntryViewerStandIn
import mihon.entry.interactions.runtime.EntryTypeKey

/** Desktop has no player yet, so episodes stay openable and opening one says watching isn't here yet. */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeViewerStandInBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun viewer(): EntryViewerStandIn = EntryViewerStandIn
}
