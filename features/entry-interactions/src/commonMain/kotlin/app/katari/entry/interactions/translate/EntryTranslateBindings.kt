package app.katari.entry.interactions.translate

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.power.PowerSource
import app.katari.entry.interactions.download.EntryAutomaticDownloadFeature
import app.katari.entry.interactions.download.EntryDownloadProcessor
import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.translate.ahead.EntryTranslateAhead
import app.katari.entry.interactions.translate.download.EntryTranslateDownloadChaining
import app.katari.entry.interactions.translate.download.EntryTranslateDownloadRules
import app.katari.entry.interactions.translate.work.EntryTranslateConditions
import app.katari.entry.interactions.translate.work.EntryTranslateQueueRunner
import app.katari.entry.interactions.translate.work.EntryTranslateRun
import app.katari.entry.interactions.translate.work.EntryTranslateWorkController
import app.katari.entry.interactions.translation.EntryTranslationLanguagesFeature
import app.katari.translation.api.host.TranslationHostActions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryTranslateBindings {
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
    internal fun conditions(
        power: PowerSource,
        network: NetworkConnectivity,
        preferences: EntryTranslatePreferences,
        repository: EntryTranslationQueueRepository,
        hostActions: () -> TranslationHostActions,
    ): EntryTranslateConditions = EntryTranslateConditions(
        power = power,
        network = network,
        preferences = preferences,
        repository = repository,
        engines = { hostActions().knownEngines },
    )

    @Provides
    internal fun run(
        runner: EntryTranslateQueueRunner,
        conditions: EntryTranslateConditions,
    ): EntryTranslateRun = EntryTranslateRun(runner = runner, conditions = conditions)

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
    ): StartupWarmup = StartupWarmup {
        val appScope = scope()
        appScope.launch { downloadChaining().run() }
        appScope.launch { downloadRules().run() }
    }
}
