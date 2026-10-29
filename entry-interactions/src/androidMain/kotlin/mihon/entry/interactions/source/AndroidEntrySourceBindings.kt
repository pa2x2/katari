package mihon.entry.interactions.source

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidEntrySourceBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun webViewFeature(
        sourceManager: SourceManager,
    ): EntryWebViewFeature = DefaultEntryWebViewFeature(
        sourceManager = sourceManager,
    )
}
