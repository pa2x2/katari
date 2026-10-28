package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.media.NoEntryPreviewFeature
import mihon.entry.interactions.media.EntryPreviewFeature

@BindingContainer
interface MediaBindings {
    @Binds
    val NoEntryPreviewFeature.entryPreviewFeature: EntryPreviewFeature
}
