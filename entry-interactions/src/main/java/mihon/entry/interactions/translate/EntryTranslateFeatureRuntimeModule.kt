package mihon.entry.interactions.translate

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.entry.interactions.translate.ahead.EntryTranslateAhead
import mihon.entry.interactions.translate.ahead.EntryTranslateAheadContributor
import mihon.entry.interactions.translate.ahead.entryTranslateAheadMediaSessionBinding
import mihon.entry.interactions.translate.download.EntryTranslateDownloadChaining
import mihon.entry.interactions.translate.download.EntryTranslateDownloadRules
import mihon.entry.interactions.translate.work.DefaultEntryTranslateWorkController
import mihon.entry.interactions.translate.work.EntryTranslateConditions
import mihon.entry.interactions.translate.work.EntryTranslateNotifier
import mihon.entry.interactions.translate.work.EntryTranslateQueueRunner
import mihon.entry.interactions.translate.work.EntryTranslateWorkController
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.translation.api.host.TranslationHostActions
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryTranslateFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.translate",
    contributor = EntryTranslateFeatureContributor,
    additionalContributors = listOf(EntryTranslateAheadContributor),
) { context ->
    addSingletonFactory<EntryTranslateWorkController> {
        DefaultEntryTranslateWorkController(context.application)
    }
    addSingletonFactory {
        EntryTranslateQueueRunner(
            repository = get(),
            translate = get<EntryInteractions>().translate,
            entries = get(),
            chapters = get(),
        )
    }
    addSingletonFactory {
        EntryTranslateNotifier(
            context = context.application,
            actions = context.dependencies.notificationActions,
            hideContent = { get<SecurityPreferences>().hideNotificationContent.get() },
        )
    }
    addSingletonFactory {
        EntryTranslateConditions(
            context = context.application,
            preferences = get(),
            repository = get(),
            engines = { get<TranslationHostActions>().knownEngines },
        )
    }
    addSingletonFactory<EntryTranslateFeature> {
        DefaultEntryTranslateFeature(
            evaluation = get<FeatureRuntimeComposition>().evaluation,
            repository = get(),
            translate = get<EntryInteractions>().translate,
            languages = get(),
            download = get<EntryInteractions>().download,
            entries = get(),
            chapters = get(),
            runner = get(),
            work = get(),
            conditions = get(),
            preferences = get(),
        )
    }
    addSingletonFactory {
        EntryTranslateDownloadChaining(
            repository = get(),
            download = get<EntryInteractions>().download,
            entries = get(),
            chapters = get(),
            onReady = get<EntryTranslateWorkController>()::start,
        )
    }
    addSingletonFactory {
        EntryTranslateDownloadRules(
            download = get<EntryInteractions>().download,
            automaticDownload = get(),
            preferences = get(),
            languages = get(),
            feature = get(),
            repository = get(),
            entries = get(),
            chapters = get(),
        )
    }
    addSingletonFactory {
        EntryTranslateAhead(
            preferences = get(),
            feature = get(),
            download = get<EntryInteractions>().download,
            getEntryWithChapters = get(),
            entries = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(entryTranslateAheadMediaSessionBinding { get<EntryTranslateAhead>() }),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryTranslateFeature>() }),
        warmups = listOf {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch { get<EntryTranslateDownloadChaining>().run() }
            scope.launch { get<EntryTranslateDownloadRules>().run() }
        },
    )
}
