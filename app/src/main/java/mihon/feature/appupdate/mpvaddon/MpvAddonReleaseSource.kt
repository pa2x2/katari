package mihon.feature.appupdate.mpvaddon

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import mihon.feature.appupdate.check.GithubAsset
import mihon.feature.appupdate.check.GithubRelease
import mihon.feature.appupdate.check.GithubReleaseSource
import mihon.feature.appupdate.check.UpdateApk
import mihon.feature.appupdate.check.sha256
import mihon.video.engine.mpv.addon.MpvLoadOrder
import okhttp3.CacheControl
import java.io.IOException

/**
 * Finds mpv add-ons in the libmpv repository's releases. Each `libmpv-<version>` release has the add-on for every ABI
 * and its `load-order.json`, which says the add-on's versionCode and JNI glue level.
 */
internal class MpvAddonReleaseSource(
    private val releases: GithubReleaseSource,
    private val network: NetworkHelper,
    private val json: Json,
) {

    /**
     * The newest add-on with [glue] and at least [minVersionCode], for [abi]; null when none is published. Add-on-only
     * releases keep the glue level, and a newer level needs a newer Katari, so the newest release isn't always the one.
     */
    suspend fun newest(glue: Int, minVersionCode: Long, abi: String): MpvAddonRelease? {
        val candidates = releases.releasesAt(RELEASES_URL)
            .filter { !it.draft && !it.prerelease && it.tagName.startsWith(TAG_PREFIX) }
            .take(MAX_RELEASES_READ)
        for (release in candidates) {
            val version = release.tagName.removePrefix(TAG_PREFIX)
            val loadOrder = release.asset("libmpv-android-$version-load-order.json")
                ?.let { fetch(it.downloadUrl) }
                ?.let(MpvLoadOrder::parse)
                ?: continue
            if (loadOrder.glue != glue || loadOrder.versionCode < minVersionCode) continue
            val apk = release.asset("katari-mpv-$abi-$version.apk") ?: continue
            return MpvAddonRelease(
                version = version,
                versionCode = loadOrder.versionCode,
                glue = loadOrder.glue,
                apk = UpdateApk(name = apk.name, url = apk.downloadUrl, size = apk.size, sha256 = apk.sha256),
                components = release.asset("libmpv-android-$version-third-party.json")?.let {
                    components(it.downloadUrl)
                },
            )
        }
        return null
    }

    /** What a Katari release needs of the add-on, from its `katari-<tag>-mpv.json`; null when unreadable. */
    suspend fun katariRequirement(url: String): KatariMpvRequirement? = try {
        json.decodeFromString<KatariMpvRequirement>(fetch(url)).takeIf { it.format == KatariMpvRequirement.FORMAT }
    } catch (e: CancellationException) {
        throw e
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun GithubRelease.asset(name: String): GithubAsset? = assets.find { it.name == name }

    /** "mpv 0.41.0, FFmpeg 9.0.2" from the release's licence inventory; null when it can't be read. */
    private suspend fun components(url: String): String? = try {
        val inventory = json.decodeFromString<ComponentInventory>(fetch(url))
        listOf("mpv", "FFmpeg")
            .mapNotNull { name ->
                inventory.components.find { it.name == name }
                    ?.let { "$name ${it.version.substringBefore(' ')}" }
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun fetch(url: String): String {
        val response = try {
            network.client.newCall(GET(url, cache = CacheControl.FORCE_NETWORK)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            throw AppUpdateException(AppUpdateError.GithubUnreachable)
        }
        return response.use {
            if (!it.isSuccessful) throw AppUpdateException(AppUpdateError.GithubStatus(it.code))
            try {
                it.body.string()
            } catch (_: IOException) {
                throw AppUpdateException(AppUpdateError.GithubUnreachable)
            }
        }
    }

    @Serializable
    private data class ComponentInventory(val components: List<Component> = emptyList())

    @Serializable
    private data class Component(val name: String, val version: String)

    private companion object {
        const val REPOSITORY = "pa2x2/mpv-android"
        const val TAG_PREFIX = "libmpv-"

        /** Each release read costs a small download, and only a Katari that is far behind looks past the first few. */
        const val MAX_RELEASES_READ = 10

        val RELEASES_URL: String = BuildConfig.MPV_ADDON_RELEASES_URL
            .ifEmpty { "https://api.github.com/repos/$REPOSITORY/releases?per_page=30" }
    }
}

/** `katari-<tag>-mpv.json`, written by the release workflow from the AAR's `load-order.json`. */
@Serializable
internal data class KatariMpvRequirement(
    val format: Int,
    val glue: Int,
    val minVersionCode: Long,
) {
    companion object {
        const val FORMAT = 1
    }
}
