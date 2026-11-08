package app.katari.entry.interactions.host

import app.katari.entry.interactions.download.EntryDownloadNotificationActions
import app.katari.entry.interactions.runtime.EntryInteractionActivityTheme
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.entry.AppEntryDownloadNotificationActions
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl

/** The services Entry Features need from the Android app alone: its activity theme and download notifications. */
@BindingContainer
object AndroidEntryHostBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun activityTheme(): EntryInteractionActivityTheme =
        EntryInteractionActivityTheme(ThemingDelegateImpl()::applyAppTheme)

    @Provides
    @SingleIn(AppScope::class)
    fun notificationActions(): EntryDownloadNotificationActions = AppEntryDownloadNotificationActions()
}
