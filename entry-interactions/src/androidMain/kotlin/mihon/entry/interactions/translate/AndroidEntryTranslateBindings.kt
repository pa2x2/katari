package mihon.entry.interactions.translate

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.translate.notification.EntryTranslateNotifier
import mihon.entry.interactions.translate.notification.EntryTranslatePausedNotification
import mihon.entry.interactions.translate.work.DefaultEntryTranslateWorkController
import mihon.entry.interactions.translate.work.EntryTranslateConditions
import mihon.entry.interactions.translate.work.EntryTranslateQueueRunner
import mihon.entry.interactions.translate.work.EntryTranslateWorkController

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidEntryTranslateBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun workController(application: Application): EntryTranslateWorkController =
        DefaultEntryTranslateWorkController(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun notifier(
        application: Application,
        actions: EntryDownloadNotificationActions,
        securityPreferences: () -> SecurityPreferences,
    ): EntryTranslateNotifier = EntryTranslateNotifier(
        context = application,
        actions = actions,
        hideContent = { securityPreferences().hideNotificationContent.get() },
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun pausedNotification(
        conditions: EntryTranslateConditions,
        runner: EntryTranslateQueueRunner,
        notifier: EntryTranslateNotifier,
    ): EntryTranslatePausedNotification = EntryTranslatePausedNotification(
        conditions = conditions,
        runner = runner,
        notifier = notifier,
    )

    @Provides
    @IntoSet
    internal fun pausedNotificationWarmup(
        @AppCoroutineScope scope: () -> CoroutineScope,
        pausedNotification: () -> EntryTranslatePausedNotification,
    ): StartupWarmup = StartupWarmup {
        scope().launch { pausedNotification().run() }
    }
}
