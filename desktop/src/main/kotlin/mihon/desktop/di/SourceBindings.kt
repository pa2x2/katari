package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.domain.source.service.ProfileHiddenSourceIds
import mihon.desktop.source.NoSourceManager
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager

@BindingContainer
interface SourceBindings {
    @Binds
    val ProfileHiddenSourceIds.hiddenSourceIds: HiddenSourceIds

    @Binds
    val NoSourceManager.sourceManager: SourceManager
}
