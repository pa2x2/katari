package mihon.entry.interactions.anime

import android.app.Application
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.AnimeDownloader
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

fun InjektRegistrar.addAnimeEntryInteractionRuntime(app: Application): () -> Unit {
    addAnimePlayerRuntime(app)
    addSingletonFactory { AnimeDownloadProvider(app) }
    addSingletonFactory { AnimeDownloadCache(app) }
    addSingletonFactory { AnimeDownloader(get(), get(), get(), get(), get(), get()) }
    addSingletonFactory { AnimeDownloadManager(app, get(), get(), get(), get()) }

    return { get<AnimeDownloadManager>() }
}
