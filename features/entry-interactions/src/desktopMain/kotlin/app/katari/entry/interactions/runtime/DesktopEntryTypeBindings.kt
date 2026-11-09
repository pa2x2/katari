package app.katari.entry.interactions.runtime

import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.translate.EntryTranslateProcessor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * The registries that only the entry types' readers and player fill. Every type module compiles for both platforms,
 * but those parts of each are Android-only, so each registry is declared empty here and the shared features answer for
 * every entry as for a type they don't apply to. The rest, such as reading and watch state, the chapter list, the
 * chapter continuing opens, library progress, labels, statistics, playback preferences and downloads, the type modules
 * contribute on both platforms, so those stay undeclared and a type that stops contributing them fails to build on both.
 *
 * - Opening, previews, the immersive view and the media caches they fill need the readers and the player. Desktop has
 *   the anime player and the manga and book readers, which their types' desktop parts bind as their open processors,
 *   anime's clips and manga's chapters in the immersive view, manga's previews and page cache, and the book's
 *   materialized resources. A type whose viewer a platform lacks binds an `EntryViewerStandIn` in its place.
 * - Translating downloaded chapters goes through the manga reader's page store, so manga binds it on both platforms
 *   and no other type does.
 */
@ContributesTo(AppScope::class)
@BindingContainer
interface DesktopEntryTypeBindings {
    @Multibinds(allowEmpty = true)
    fun openProcessors(): Map<EntryType, EntryOpenProcessor>

    @Multibinds(allowEmpty = true)
    fun previewProcessors(): Map<EntryType, EntryPreviewProcessor>

    @Multibinds(allowEmpty = true)
    fun previewConfigurationProviders(): Map<EntryType, EntryPreviewConfigurationProvider>

    @Multibinds(allowEmpty = true)
    fun immersiveProcessors(): Map<EntryType, EntryImmersiveProcessor>

    @Multibinds(allowEmpty = true)
    fun mediaCacheProviders(): Map<EntryType, EntryMediaCacheProvider>

    @Multibinds(allowEmpty = true)
    fun translateProcessors(): Map<EntryType, EntryTranslateProcessor>
}
