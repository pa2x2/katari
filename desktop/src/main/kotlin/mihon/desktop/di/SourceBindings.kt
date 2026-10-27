package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import tachiyomi.domain.source.service.HiddenSourceIds

@BindingContainer
interface SourceBindings {
    @Binds
    val ProfileHiddenSourceIds.hiddenSourceIds: HiddenSourceIds
}
