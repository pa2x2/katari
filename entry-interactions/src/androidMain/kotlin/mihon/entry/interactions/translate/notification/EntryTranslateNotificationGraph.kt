package mihon.entry.interactions.translate.notification

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.translate.EntryTranslateFeature

/** Graph entry points of the translation notification buttons' receiver, which Android constructs. */
@ContributesTo(AppScope::class)
interface EntryTranslateNotificationGraph {
    val entryTranslateFeature: EntryTranslateFeature

    @AppCoroutineScope
    val appScope: CoroutineScope
}
