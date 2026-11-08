package app.katari.entry.interactions.translate.notification

import app.katari.core.metro.AppCoroutineScope
import app.katari.entry.interactions.translate.EntryTranslateFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.CoroutineScope

/** Graph entry points of the translation notification buttons' receiver, which Android constructs. */
@ContributesTo(AppScope::class)
interface EntryTranslateNotificationGraph {
    val entryTranslateFeature: EntryTranslateFeature

    @AppCoroutineScope
    val appScope: CoroutineScope
}
