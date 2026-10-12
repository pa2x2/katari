package eu.kanade.tachiyomi.data.track.suwayomi

import android.content.SharedPreferences
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.graphql.dataOrElse
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.coroutines.Dispatchers
import mihon.entry.interactions.source.EntryTrackerSourceAdapterFeature
import mihon.entry.interactions.source.EntryTrackerSourceAdapterResolution
import mihon.graphql.suwayomi.SuwayomiGetMangaQuery
import mihon.graphql.suwayomi.SuwayomiGetMangaUnreadChaptersQuery
import mihon.graphql.suwayomi.SuwayomiMarkAndDeleteChaptersMutation
import mihon.graphql.suwayomi.SuwayomiMarkChaptersReadMutation
import mihon.graphql.suwayomi.SuwayomiUpdateMangaProgressMutation
import uy.kohesive.injekt.injectLazy
import java.security.MessageDigest

class SuwayomiApi(private val trackerId: Long) {

    private val sourceAdapterFeature: EntryTrackerSourceAdapterFeature by injectLazy()
    private val sourceAdapter: EntryTrackerSourceAdapterResolution.Available by lazy {
        sourceAdapterFeature.resolve(sourceId) as? EntryTrackerSourceAdapterResolution.Available
            ?: error("Source $sourceId does not provide the Suwayomi tracker connection")
    }
    private val baseUrl: String by lazy { sourceAdapter.homeUrl.trimEnd('/') }
    private val graphQlClient: ApolloClient by lazy {
        ApolloClient.Builder()
            .serverUrl("$baseUrl/api/graphql")
            .okHttpClient(sourceAdapter.imageClient)
            .dispatcher(Dispatchers.IO)
            // required to log the error body in dataOrElse, which also properly closes it
            .httpExposeErrorBody(true)
            .build()
    }

    fun sourcePreferences(): SharedPreferences = sourceAdapter.preferences

    suspend fun getTrackSearch(mangaId: Long): TrackSearch? {
        return graphQlClient
            .query(
                SuwayomiGetMangaQuery(mangaId = mangaId.toInt()),
            )
            .execute()
            .dataOrElse(
                errorLog = "Suwayomi: Failed to find manga in library",
                default = { null },
            ) {
                it.manga.mangaFragment.toTrackSearch(trackerId, baseUrl)
            }
    }

    suspend fun updateProgress(track: Track, deleteDownloadsOnServer: Boolean = false): Track? {
        val mangaId = track.remote_id

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

    private val sourceId by lazy {
        val key = "tachidesk/en/1"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }
}
