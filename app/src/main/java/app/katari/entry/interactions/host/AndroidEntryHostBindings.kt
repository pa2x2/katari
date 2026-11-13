package app.katari.entry.interactions.host

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.katari.android.di.appGraph
import app.katari.entry.interactions.download.EntryDownloadNotificationActions
import app.katari.entry.interactions.runtime.EntryInteractionActivityTheme
import app.katari.entry.interactions.runtime.EntryInteractionCompositionRoot
import app.katari.ui.host.AppUiRoot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.entry.AppEntryDownloadNotificationActions
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import eu.kanade.tachiyomi.util.view.TestTagsAsResourceIds

/**
 * The services Entry Features need from the Android app alone: its activity theme, the root of its viewers'
 * compositions and download notifications.
 */
@BindingContainer
object AndroidEntryHostBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun activityTheme(): EntryInteractionActivityTheme =
        EntryInteractionActivityTheme(ThemingDelegateImpl()::applyAppTheme)

    @Provides
    @SingleIn(AppScope::class)
    fun compositionRoot(): EntryInteractionCompositionRoot = object : EntryInteractionCompositionRoot {
        @Composable
        override fun Content(content: @Composable () -> Unit) {
            AppUiRoot(LocalContext.current.appGraph) { TestTagsAsResourceIds(content) }
        }
    }

    @Provides
    @SingleIn(AppScope::class)
    fun notificationActions(): EntryDownloadNotificationActions = AppEntryDownloadNotificationActions()
}
