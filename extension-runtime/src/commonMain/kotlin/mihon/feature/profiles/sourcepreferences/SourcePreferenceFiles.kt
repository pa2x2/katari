package mihon.feature.profiles.sourcepreferences

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Owns where source settings live: each profile has its own file per source, [sourcePreferenceFileName], named by the
 * ID the extension itself knows, whatever local key Katari stores the source under.
 *
 * Extensions pick their file by name, opening [extensionPreferenceFileName] on the injected Application. The app hands
 * those names to [forSource], whose [ProfileResolvingSharedPreferences] reads the right profile's file on each call. The
 * settings screen (through [ProfileSourcePreferenceProvider]), backups and restore open the same files here, so the
 * first open of a source moves its file from before profiles owned them into every profile before anything reads it.
 *
 * [watchWrites] hears what is written to the files from then on, by the extensions or anything else. A file is watched only
 * once its source's file from before profiles is moved, so the move itself is never heard.
 */
class SourcePreferenceFiles(
    private val fileStore: SourcePreferenceFileStore,
    private val profileStore: ProfileStore,
    private val profileDatabase: ProfileDatabase,
    private val identities: SourceIdentityRepository,
    scope: CoroutineScope,
) {
    private val resolving = ConcurrentHashMap<Long, ProfileResolvingSharedPreferences>()
    private val movedSourceIds = ConcurrentHashMap.newKeySet<Long>()
    private val openedFiles = ConcurrentHashMap<String, Pair<Long, Long>>()
    private val writeObservers = CopyOnWriteArraySet<(SourceSettingWrite) -> Unit>()

    // Shared preferences hold their listeners weakly, so these keep them registered.
    private val watchers = ConcurrentHashMap<String, SharedPreferences.OnSharedPreferenceChangeListener>()

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
        if (sourceId !in movedSourceIds) moveSharedFile(sourceId)
        val name = sourcePreferenceFileName(profileId, sourceId)
        openedFiles[name] = profileId to sourceId
        return file(profileId, sourceId).also { if (writeObservers.isNotEmpty()) watch(name, it) }
    }

    /**
     * Calls [listener] with every write to any profile's source settings, from every writer, as each happens, until
     * the returned handle is closed. Nothing listens to the files while nobody watches.
     */
    fun watchWrites(listener: (SourceSettingWrite) -> Unit): AutoCloseable {
        writeObservers += listener
        fileStore.names().mapNotNull(::sourcePreferenceFileOwner).forEach { (profileId, sourceId) ->
            open(profileId, sourceId)
        }
        openedFiles.forEach { (name, owner) -> watch(name, file(owner.first, owner.second)) }
        return AutoCloseable {
            writeObservers -= listener
            if (writeObservers.isEmpty()) stopWatching()
        }
    }

    private fun watch(name: String, file: SharedPreferences) {
        val (profileId, sourceId) = openedFiles[name] ?: return
        watchers.computeIfAbsent(name) {
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                val write = SourceSettingWrite(profileId, sourceId, key)
                writeObservers.forEach { it(write) }
            }.also(file::registerOnSharedPreferenceChangeListener)
        }
    }

    private fun stopWatching() {
        watchers.keys.toList().forEach { name ->
            val listener = watchers.remove(name) ?: return@forEach
            val (profileId, sourceId) = openedFiles[name] ?: return@forEach
            file(profileId, sourceId).unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /** Deletes [profileId]'s settings of the source with its own ID [sourceId]; other profiles keep theirs. */
    fun clear(profileId: Long, sourceId: Long) {
        open(profileId, sourceId).edit().clear().commit()
    }

    /** The ID the source stored under [localKey] knows itself by, which its settings file is named after. */
    suspend fun sourceIdOf(localKey: Long): Long {
        return (identities.find(localKey)?.identity ?: SourceIdentity.ofLegacyKey(localKey)).externalId
    }

    fun deleteProfile(profileId: Long) {
        val prefix = profileSourcePreferenceFilePrefix(profileId)
        openedFiles.keys.removeIf { it.startsWith(prefix) }
        fileStore.names()
            .filter { it.startsWith(prefix) }
            .forEach(fileStore::delete)
        resolving.values.forEach { it.forget(profileId) }
    }

    // See copySharedSourceSettings. The old file is deleted only once every profile's copy is committed.
    private fun moveSharedFile(sourceId: Long) {
        synchronized(movedSourceIds) {
            if (sourceId in movedSourceIds) return
            val name = extensionPreferenceFileName(sourceId)
            if (fileStore.hasLegacyFile(name)) {
                try {
                    val profileIds = runBlocking {
                        profileDatabase.getProfiles(includeArchived = true)
                    }.map(Profile::id)
                    copySharedSourceSettings(
                        shared = fileStore.openLegacyFile(name).all,
                        defaultProfileFile = file(ProfileConstants.DEFAULT_PROFILE_ID, sourceId),
                        profileFiles = (profileIds + ProfileConstants.DEFAULT_PROFILE_ID).distinct()
                            .map { file(it, sourceId) },
                    )
                    fileStore.deleteLegacyFile(name)
                } catch (e: Exception) {
                    // Left in place, so the next open tries again
                    logcat(LogPriority.ERROR, e) { "Failed to move the settings of source $sourceId into profiles" }
                    return
                }
            }
            movedSourceIds += sourceId
        }
    }

    private fun file(profileId: Long, sourceId: Long): SharedPreferences {
        return fileStore.open(sourcePreferenceFileName(profileId, sourceId))
    }
}
