package mihon.entry.interactions.manga

import android.app.Application
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

fun InjektRegistrar.addMangaEntryInteractionRuntime(app: Application): () -> Unit {
    addSingletonFactory { DownloadProvider(app) }
    addSingletonFactory { DownloadManager(app) }
    addSingletonFactory { DownloadCache(app) }

    return { get<DownloadManager>() }
}
