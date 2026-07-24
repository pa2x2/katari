package mihon.entry.interactions.anime

import android.app.Application
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerMediaCache
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import mihon.entry.interactions.EntryPlayerCache
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal fun InjektRegistrar.addAnimePlayerRuntime(app: Application) {
    addSingletonFactory { ResolveVideoStream(get(), get(), get()) }
    addSingletonFactory<VideoStreamResolver> { get<ResolveVideoStream>() }
    addSingletonFactory { VideoPlayerMediaCache(app) }
    addSingletonFactory<EntryPlayerCache> { get<VideoPlayerMediaCache>() }
    addSingletonFactory { AnimePlayerBasePreferences(get<PreferenceStore>()) }
}
