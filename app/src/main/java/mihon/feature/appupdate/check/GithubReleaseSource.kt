package mihon.feature.appupdate.check

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import okhttp3.CacheControl
import okhttp3.Headers
import java.io.IOException

internal class GithubReleaseSource(
    private val network: NetworkHelper,
    private val json: Json,
) {

    /** The repository's latest releases, newest first. Enough to cover anyone a few months behind. */
    suspend fun releases(repository: String): List<GithubRelease> {
        val request = GET(
            "https://api.github.com/repos/$repository/releases?per_page=30",
            Headers.headersOf("Accept", "application/vnd.github+json", "X-GitHub-Api-Version", "2022-11-28"),
            CacheControl.FORCE_NETWORK,
        )
        val response = try {
            network.client.newCall(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            throw AppUpdateException(AppUpdateError.GithubUnreachable)
        }
        return response.use {
            if ((it.code == 403 || it.code == 429) && it.header("x-ratelimit-remaining") == "0") {
                throw AppUpdateException(AppUpdateError.GithubRateLimited)
            }
            if (!it.isSuccessful) throw AppUpdateException(AppUpdateError.GithubStatus(it.code))
            try {
                json.decodeFromString<List<GithubRelease>>(it.body.string())
            } catch (_: SerializationException) {
                throw AppUpdateException(AppUpdateError.GithubUnexpected)
            } catch (_: IllegalArgumentException) {
                throw AppUpdateException(AppUpdateError.GithubUnexpected)
            } catch (_: IOException) {
                throw AppUpdateException(AppUpdateError.GithubUnreachable)
            }
        }
    }
}
