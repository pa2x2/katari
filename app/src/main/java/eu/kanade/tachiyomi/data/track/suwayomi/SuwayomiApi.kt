package eu.kanade.tachiyomi.data.track.suwayomi

import android.content.SharedPreferences
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerSourceKeys
import eu.kanade.tachiyomi.data.track.graphql.dataOrElse
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.entry.interactions.source.EntryTrackerSourceAdapterFeature
import mihon.entry.interactions.source.EntryTrackerSourceAdapterResolution
import mihon.graphql.suwayomi.SuwayomiGetMangaQuery
import mihon.graphql.suwayomi.SuwayomiGetMangaUnreadChaptersQuery
import mihon.graphql.suwayomi.SuwayomiMarkAndDeleteChaptersMutation
import mihon.graphql.suwayomi.SuwayomiMarkChaptersReadMutation
import mihon.graphql.suwayomi.SuwayomiUpdateMangaProgressMutation

class SuwayomiApi(
    private val trackerId: Long,
    private val sourceKeys: TrackerSourceKeys,
    private val sourceAdapterFeature: Lazy<EntryTrackerSourceAdapterFeature>,
) {

    private class Connection(val preferences: SharedPreferences, val baseUrl: String, val graphQlClient: ApolloClient)

    private val connecting = Mutex()
    private var connected: Connection? = null

    private suspend fun connection(): Connection = connecting.withLock {
        connected ?: connect().also { connected = it }
    }

    private suspend fun connect(): Connection {
        val sourceKey = sourceKeys.keyOf(name = "tachidesk", lang = "en", versionId = 1)
            ?: error("The Suwayomi source is not installed")
        val sourceAdapter =
            sourceAdapterFeature.value.resolve(sourceKey) as? EntryTrackerSourceAdapterResolution.Available
                ?: error("Source $sourceKey does not provide the Suwayomi tracker connection")
        val baseUrl = sourceAdapter.homeUrl.trimEnd('/')
        val graphQlClient = ApolloClient.Builder()
            .serverUrl("$baseUrl/api/graphql")
            .okHttpClient(sourceAdapter.imageClient)
            .dispatcher(Dispatchers.IO)
            // required to log the error body in dataOrElse, which also properly closes it
            .httpExposeErrorBody(true)
            .build()
        return Connection(sourceAdapter.preferences, baseUrl, graphQlClient)
    }

    suspend fun sourcePreferences(): SharedPreferences = connection().preferences

    suspend fun getTrackSearch(mangaId: Long): TrackSearch? {
        val connection = connection()
        return connection.graphQlClient
            .query(
                SuwayomiGetMangaQuery(mangaId = mangaId.toInt()),
            )
            .execute()
            .dataOrElse(
                errorLog = "Suwayomi: Failed to find manga in library",
                default = { null },
            ) {
                it.manga.mangaFragment.toTrackSearch(trackerId, connection.baseUrl)
            }
    }

    suspend fun updateProgress(track: Track, deleteDownloadsOnServer: Boolean = false): Track? {
        val mangaId = track.remote_id
        val graphQlClient = connection().graphQlClient

        val chaptersToMark = graphQlClient
            .query(
                SuwayomiGetMangaUnreadChaptersQuery(
                    mangaId = mangaId.toInt(),
                    chapterNumber = track.last_chapter_read,
                ),
            )
            .execute()
            .dataOrElse(
                errorLog = "Suwayomi: Failed to get chapters data",
                default = { null },
            ) {
                it.chapters.nodes.map { chapter -> chapter.id }
            }
            ?: throw Exception("Could not get chapters data")

        if (chaptersToMark.isEmpty()) {
            return getTrackSearch(mangaId)
        }

        val markMutation = if (deleteDownloadsOnServer) {
            SuwayomiMarkAndDeleteChaptersMutation(chapters = chaptersToMark)
        } else {
            SuwayomiMarkChaptersReadMutation(chapters = chaptersToMark)
        }

        graphQlClient
            .mutation(markMutation)
            .execute()
            .dataOrElse(
                errorLog = "Suwayomi: Failed to mark chapters",
                default = {},
            ) {}

        graphQlClient
            .mutation(
                SuwayomiUpdateMangaProgressMutation(mangaId = mangaId.toInt()),
            )
            .execute()
            .dataOrElse(
                errorLog = "Suwayomi: Failed to update progress",
                default = {},
            ) {}

        return getTrackSearch(mangaId)
    }
}
