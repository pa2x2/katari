package eu.kanade.tachiyomi.data.track

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.track.service.GlobalTrackPreferences
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.comick.Comick
import eu.kanade.tachiyomi.data.track.hikka.Hikka
import eu.kanade.tachiyomi.data.track.kavita.Kavita
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.komga.Komga
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBaka
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.suwayomi.Suwayomi
import kotlinx.coroutines.flow.combine
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.entry.interactions.source.EntryTrackerSourceAdapterFeature
import mihon.feature.profiles.core.ProfileStore

@Inject
@SingleIn(AppScope::class)
class TrackerManager(
    profileStore: ProfileStore,
    globalTrackPreferences: GlobalTrackPreferences,
    dependencies: TrackerDependencies,
    sourceKeys: TrackerSourceKeys,
    sourceSettingsFeature: Lazy<EntrySourceSettingsFeature>,
    sourceAdapterFeature: Lazy<EntryTrackerSourceAdapterFeature>,
) {

    val myAnimeList = MyAnimeList(1L, dependencies)
    val aniList = Anilist(2L, dependencies)
    val kitsu = Kitsu(3L, dependencies)
    val shikimori = Shikimori(4L, dependencies)
    val bangumi = Bangumi(5L, dependencies)
    val komga = Komga(6L, dependencies)
    val mangaUpdates = MangaUpdates(7L, dependencies)
    val kavita = Kavita(8L, dependencies, sourceKeys, sourceSettingsFeature)
    val suwayomi = Suwayomi(9L, dependencies, sourceKeys, sourceAdapterFeature)
    val hikka = Hikka(10L, dependencies, profileStore, globalTrackPreferences)
    val mangaBaka = MangaBaka(11L, dependencies, profileStore)
    val comick = Comick(12L, dependencies, profileStore)

    val trackers = listOf(
        mangaBaka,
        myAnimeList,
        aniList,
        kitsu,
        shikimori,
        bangumi,
        komga,
        mangaUpdates,
        kavita,
        suwayomi,
        hikka,
        comick,
    )

    fun loggedInTrackers() = trackers.filter { it.isLoggedIn }

    fun loggedInTrackersFlow() = combine(trackers.map { it.isLoggedInFlow }) {
        it.mapIndexedNotNull { index, isLoggedIn ->
            if (isLoggedIn) trackers[index] else null
        }
    }

    fun get(id: Long) = trackers.find { it.id == id }

    fun getAll(ids: Set<Long>) = trackers.filter { it.id in ids }
}
