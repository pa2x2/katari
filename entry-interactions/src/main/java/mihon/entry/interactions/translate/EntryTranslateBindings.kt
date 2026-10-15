package mihon.entry.interactions.translate

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.download.EntryAutomaticDownloadFeature
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.translate.ahead.EntryTranslateAhead
import mihon.entry.interactions.translate.download.EntryTranslateDownloadChaining
import mihon.entry.interactions.translate.download.EntryTranslateDownloadRules
import mihon.entry.interactions.translate.notification.EntryTranslateNotifier
import mihon.entry.interactions.translate.notification.EntryTranslatePausedNotification
import mihon.entry.interactions.translate.work.DefaultEntryTranslateWorkController
import mihon.entry.interactions.translate.work.EntryTranslateConditions
import mihon.entry.interactions.translate.work.EntryTranslateQueueRunner
import mihon.entry.interactions.translate.work.EntryTranslateWorkController
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryTranslateBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun workController(application: Application): EntryTranslateWorkController =
        DefaultEntryTranslateWorkController(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun queueRunner(
        repository: EntryTranslationQueueRepository,
        interactions: EntryInteractions,
        entries: EntryRepository,
        chapters: EntryChapterRepository,
    ): EntryTranslateQueueRunner = EntryTranslateQueueRunner(
        repository = repository,
        translate = interactions.translate,
        entries = entries,
        chapters = chapters,
    )

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
    internal fun conditions(
        application: Application,
        preferences: EntryTranslatePreferences,
        repository: EntryTranslationQueueRepository,
        hostActions: () -> TranslationHostActions,
    ): EntryTranslateConditions = EntryTranslateConditions(
        context = application,
        preferences = preferences,
        repository = repository,
        engines = { hostActions().knownEngines },
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun feature(
        translateProcessors: Map<EntryType, EntryTranslateProcessor>,
        downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
        repository: EntryTranslationQueueRepository,
        interactions: EntryInteractions,
        languages: EntryTranslationLanguagesFeature,
        entries: EntryRepository,
        chapters: EntryChapterRepository,
        runner: EntryTranslateQueueRunner,
        work: EntryTranslateWorkController,
        conditions: EntryTranslateConditions,
        preferences: EntryTranslatePreferences,
    ): EntryTranslateFeature = DefaultEntryTranslateFeature(
        applicableTypes = translateProcessors.keys intersect downloadProcessors.keys,
        repository = repository,
        translate = interactions.translate,
        languages = languages,
        download = interactions.download,
        entries = entries,
        chapters = chapters,
        runner = runner,
        work = work,
        conditions = conditions,
        preferences = preferences,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadChaining(
        repository: EntryTranslationQueueRepository,
        interactions: EntryInteractions,
        entries: EntryRepository,
        chapters: EntryChapterRepository,
        work: EntryTranslateWorkController,
    ): EntryTranslateDownloadChaining = EntryTranslateDownloadChaining(
        repository = repository,
        download = interactions.download,
        entries = entries,
        chapters = chapters,
        onReady = work::start,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadRules(
        interactions: EntryInteractions,
        automaticDownload: EntryAutomaticDownloadFeature,
        preferences: EntryTranslatePreferences,
        languages: EntryTranslationLanguagesFeature,
        feature: EntryTranslateFeature,
        repository: EntryTranslationQueueRepository,
        entries: EntryRepository,
        chapters: EntryChapterRepository,
    ): EntryTranslateDownloadRules = EntryTranslateDownloadRules(
        download = interactions.download,
        automaticDownload = automaticDownload,
        preferences = preferences,
        languages = languages,
        feature = feature,
        repository = repository,
        entries = entries,
        chapters = chapters,
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
    @SingleIn(AppScope::class)
    internal fun ahead(
        preferences: EntryTranslatePreferences,
        feature: EntryTranslateFeature,
        interactions: EntryInteractions,
        getEntryWithChapters: GetEntryWithChapters,
        entries: EntryRepository,
    ): EntryTranslateAhead = EntryTranslateAhead(
        preferences = preferences,
        feature = feature,
        download = interactions.download,
        getEntryWithChapters = getEntryWithChapters,
        entries = entries,
    )

    @Provides
    @IntoSet
    internal fun backgroundWorkWarmup(
        @AppCoroutineScope scope: () -> CoroutineScope,
        downloadChaining: () -> EntryTranslateDownloadChaining,
        downloadRules: () -> EntryTranslateDownloadRules,
        pausedNotification: () -> EntryTranslatePausedNotification,
    ): StartupWarmup = StartupWarmup {
        val appScope = scope()
        appScope.launch { downloadChaining().run() }
        appScope.launch { downloadRules().run() }
        appScope.launch { pausedNotification().run() }
    }
}
