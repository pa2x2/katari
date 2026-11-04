package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.translate.EntryTranslateProcessor

/**
 * The registries that only the entry types' readers and player fill. Every type module compiles for both platforms,
 * but those parts of each are Android-only, so each registry is declared empty here and the shared features answer for
 * every entry as for a type they don't apply to. The rest, such as reading and watch state, the chapter list, the
 * chapter continuing opens, library progress, labels, statistics, playback preferences and downloads, the type modules
 * contribute on both platforms, so those stay undeclared and a type that stops contributing them fails to build on both.
 *
 * - Opening, previews, the immersive view and the media caches they fill need the readers and the player. Desktop has
 *   the anime player, which anime's desktop part binds as its open processor; the readers' types bind an
 *   `EntryViewerStandIn` in its place, so chapters stay openable and opening one says the reader isn't here yet (D072).
 * - Translating downloaded chapters goes through the manga reader's page store, so it applies to no type yet either.
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
