package app.katari.entry.interactions.translate

import android.app.Application
import androidx.work.ListenableWorker
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.entry.interactions.download.EntryDownloadNotificationActions
import app.katari.entry.interactions.translate.notification.EntryTranslateNotifier
import app.katari.entry.interactions.translate.notification.EntryTranslatePausedNotification
import app.katari.entry.interactions.translate.work.DefaultEntryTranslateWorkController
import app.katari.entry.interactions.translate.work.EntryTranslateConditions
import app.katari.entry.interactions.translate.work.EntryTranslateJob
import app.katari.entry.interactions.translate.work.EntryTranslateQueueRunner
import app.katari.entry.interactions.translate.work.EntryTranslateWorkController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidEntryTranslateBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun workController(application: Application): EntryTranslateWorkController =
        DefaultEntryTranslateWorkController(application)

    @Provides
    @IntoMap
    @StringKey("mihon.entry.interactions.translate.work.EntryTranslateJob")
    fun retiredEntryTranslateJob(): KClass<out ListenableWorker> = EntryTranslateJob::class

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
