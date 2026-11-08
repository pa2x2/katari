package app.katari.sync.client

import app.katari.extension.runtime.jvm.preferences.SharedPreferencesDirectory
import app.katari.profiles.core.PreferenceWrite
import app.katari.profiles.core.ProfileDatabase
import app.katari.profiles.core.ProfileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import app.katari.sync.client.setting.apply.StoreSettingWrites
import app.katari.sync.client.setting.read.StoreSettingReads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.source.identity.SourceIdentityRepositoryImpl
import tachiyomi.domain.library.service.LibraryPreferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor

/**
 * One device's preferences in files under [directory], as the desktop keeps them: the shared store, a store per
 * profile, and source settings by Android's file names, with the files sources had before profiles in `legacy`. The
 * library's preferences are registered as a profile's, as the app registers them.
 */
internal class TestPreferenceStores(directory: File, handler: DatabaseHandler, scope: CoroutineScope) {
    val profiles = FileProfileStore(directory.resolve("profiles"))
    val legacySourceFiles = SharedPreferencesDirectory(directory.resolve("legacy").toPath(), Executor(Runnable::run))
    val sourceFiles = SourcePreferenceFiles(
        fileStore = LegacyAwareSourceFileStore(
            SharedPreferencesDirectory(directory.resolve("sources").toPath(), Executor(Runnable::run)),
            legacySourceFiles,
        ),
        profileStore = profiles,
        profileDatabase = ProfileDatabase(handler),
        identities = SourceIdentityRepositoryImpl(handler),
        scope = scope,
    )
    private val owners = ProfilePreferenceOwnerRegistry().apply {
        ProfilePreferenceOwnerInstaller(this) { profiles.profileStore() }
            .register(ProfilePreferenceOwnerId("app.library")) {
                LibraryPreferences(it)
            }
    }
    val reads = StoreSettingReads(profiles, sourceFiles, owners)
    val writes = StoreSettingWrites(profiles, sourceFiles)
}

/** The shared store in `base.json` and each profile's in `<id>.json`, each telling who listens of its writes. */
internal class FileProfileStore(private val directory: File) : ProfileStore {
    private val stores = ConcurrentHashMap<String, DesktopPreferenceStore>()
    private val listeners = CopyOnWriteArraySet<(PreferenceWrite) -> Unit>()

    override val currentProfileId: Long = 1
    override val currentProfileIdFlow: Flow<Long> = MutableStateFlow(1L)

    override fun setCurrentProfileId(profileId: Long) = throw UnsupportedOperationException()

    override fun basePreferenceStore(): PreferenceStore = store("base", profileId = null)

    override fun profileStore(): PreferenceStore = profileStore(currentProfileId)

    override fun profileStore(profileId: Long): PreferenceStore = store("$profileId", profileId)

    override fun appStateStore(): PreferenceStore = throw UnsupportedOperationException()

    override fun appStateStore(profileId: Long): PreferenceStore = throw UnsupportedOperationException()

    override fun privateStore(): PreferenceStore = throw UnsupportedOperationException()

    override fun privateStore(profileId: Long): PreferenceStore = throw UnsupportedOperationException()

    override fun isProfilePreference(preference: Preference<*>): Boolean = throw UnsupportedOperationException()

    override fun watchPreferenceWrites(listener: (PreferenceWrite) -> Unit): AutoCloseable {
        listeners += listener
        return AutoCloseable { listeners -= listener }
    }

    override fun deleteProfileState(profileId: Long) {
        stores.remove("$profileId")
        directory.resolve("$profileId.json").delete()
    }

    private fun store(name: String, profileId: Long?): DesktopPreferenceStore = stores.computeIfAbsent(name) {
        DesktopPreferenceStore(directory.resolve("$name.json").toPath()).apply {
            addWriteListener { key -> listeners.forEach { it(PreferenceWrite(profileId, key)) } }
        }
    }
}

private class LegacyAwareSourceFileStore(
    private val files: SharedPreferencesDirectory,
    private val legacy: SharedPreferencesDirectory,
) : SourcePreferenceFileStore {
    override fun open(name: String) = files.open(name)

    override fun names() = files.names()

    override fun delete(name: String) {
        files.delete(name)
    }

    override fun hasLegacyFile(name: String) = name in legacy.names()

    override fun openLegacyFile(name: String) = legacy.open(name)

    override fun deleteLegacyFile(name: String) {
        legacy.delete(name)
    }
}
