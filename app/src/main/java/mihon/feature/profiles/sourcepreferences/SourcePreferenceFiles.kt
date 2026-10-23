package mihon.feature.profiles.sourcepreferences

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns where source settings live: each profile has its own file per source, [sourcePreferenceFileName], named by the
 * ID the extension itself knows, whatever local key Katari stores the source under.
 *
 * Extensions pick their file by name, opening [extensionPreferenceFileName] on the injected Application. The App hands
 * those names to [forSource], whose [ProfileResolvingSharedPreferences] reads the right profile's file on each call. The
 * settings screen (through [ProfileSourcePreferenceProvider]), backups and restore open the same files here.
 */
class SourcePreferenceFiles(
    private val application: Application,
    private val profileStore: ProfileStore,
    private val identities: SourceIdentityRepository,
    scope: CoroutineScope,
) {
    private val resolving = ConcurrentHashMap<Long, ProfileResolvingSharedPreferences>()

    init {
        profileStore.currentProfileIdFlow
            .onEach { profileId -> resolving.values.forEach { it.onActiveProfileChanged(profileId) } }
            .launchIn(scope + Dispatchers.Main)
    }

    /** The settings of the source with its own ID [sourceId], for whichever profile each call works for. */
    fun forSource(sourceId: Long): SharedPreferences {
        return resolving.computeIfAbsent(sourceId) {
            ProfileResolvingSharedPreferences(
                open = { profileId -> open(profileId, sourceId) },
                activeProfileId = { profileStore.currentProfileId },
            )
        }
    }

    /** [profileId]'s settings file of the source with its own ID [sourceId]. */
    fun open(profileId: Long, sourceId: Long): SharedPreferences {
        return application.getSharedPreferences(sourcePreferenceFileName(profileId, sourceId), Context.MODE_PRIVATE)
    }

    /** The ID the source stored under [localKey] knows itself by, which its settings file is named after. */
    suspend fun sourceIdOf(localKey: Long): Long {
        return (identities.find(localKey)?.identity ?: SourceIdentity.ofLegacyKey(localKey)).externalId
    }

    fun deleteProfile(profileId: Long) {
        val prefix = profileSourcePreferenceFilePrefix(profileId)
        File(application.applicationInfo.dataDir, "shared_prefs").listFiles()
            ?.asSequence()
            ?.filter { it.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            ?.filter { it.startsWith(prefix) }
            ?.forEach(application::deleteSharedPreferences)
        resolving.values.forEach { it.forget(profileId) }
    }
}
