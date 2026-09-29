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
import mihon.feature.runtime.FeatureRuntimeComposition
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryTranslateFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.translate",
    contributor = EntryTranslateFeatureContributor,
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
    addSingletonFactory<EntryTranslateFeature> {
        DefaultEntryTranslateFeature(
            evaluation = get<FeatureRuntimeComposition>().evaluation,
            repository = get(),
            translate = get<EntryInteractions>().translate,
            download = get<EntryInteractions>().download,
            entries = get(),
            chapters = get(),
            runner = get(),
            work = get(),
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
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryTranslateFeature>() }),
        warmups = listOf {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { get<EntryTranslateDownloadChaining>().run() }
        },
    )
}
