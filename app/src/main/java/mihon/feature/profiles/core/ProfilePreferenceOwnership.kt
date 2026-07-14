package mihon.feature.profiles.core
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.serialization.json.Json
import mihon.core.common.CustomPreferences
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences

object ProfilePreferenceOwnership {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    data class Keys(
        val profile: Set<String>,
        val appState: Set<String>,
        val private: Set<String>,
    )

    fun derive(): Keys {
        val recorder = KeyRecordingPreferenceStore()

        SourcePreferences(recorder, json)
        SecurityPreferences(recorder)
        LibraryPreferences(recorder)
        DuplicatePreferences(recorder)
        UpdatesPreferences(recorder)
        EntryInteractionPreferences(recorder)
        AnimePlayerPreferences(recorder)
        MangaReaderSettingsProvider(recorder)
        UiPreferences(recorder)
        CustomPreferences(recorder)
        TrackPreferences(recorder).recordKeys(TrackerManager.TRACKER_IDS)

        return Keys(
            profile = recorder.profileKeys,
            appState = recorder.appStateKeys,
            private = recorder.privateKeys,
        )
    }

    private class KeyRecordingPreferenceStore : PreferenceStore {
        val profileKeys = linkedSetOf<String>()
        val appStateKeys = linkedSetOf<String>()
        val privateKeys = linkedSetOf<String>()

        override fun getString(key: String, defaultValue: String): Preference<String> = record(key, defaultValue)

        override fun getLong(key: String, defaultValue: Long): Preference<Long> = record(key, defaultValue)

        override fun getInt(key: String, defaultValue: Int): Preference<Int> = record(key, defaultValue)

        override fun getFloat(key: String, defaultValue: Float): Preference<Float> = record(key, defaultValue)

        override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = record(key, defaultValue)

        override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> = record(
            key,
            defaultValue,
        )

        override fun <T> getObjectFromString(
            key: String,
            defaultValue: T,
            serializer: (T) -> String,
            deserializer: (String) -> T,
        ): Preference<T> = record(key, defaultValue)

        override fun <T> getObjectFromInt(
            key: String,
            defaultValue: T,
            serializer: (T) -> Int,
            deserializer: (Int) -> T,
        ): Preference<T> = record(key, defaultValue)

        override fun <T> getObjectSetFromStringSet(
            key: String,
            defaultValue: Set<T>,
            serializer: (T) -> String,
            deserializer: (String) -> T?,
        ): Preference<Set<T>> = record(key, defaultValue)

        override fun getAll(): Map<String, *> = emptyMap<String, Any?>()

        private fun <T> record(key: String, defaultValue: T): Preference<T> {
            when {
                Preference.isPrivate(key) -> privateKeys += Preference.stripPrivateKey(key)
                Preference.isAppState(key) -> appStateKeys += Preference.stripAppStateKey(key)
                else -> profileKeys += key
            }

            return RecordedPreference(key, defaultValue)
        }
    }

    private class RecordedPreference<T>(
        private val key: String,
        private val defaultValue: T,
    ) : Preference<T> {
        override fun key(): String = key

        override fun get(): T = defaultValue

        override fun set(value: T) = Unit

        override fun isSet(): Boolean = false

        override fun delete() = Unit

        override fun defaultValue(): T = defaultValue

        override fun changes() = throw UnsupportedOperationException("Recording-only preference")

        override fun stateIn(scope: kotlinx.coroutines.CoroutineScope) =
            throw UnsupportedOperationException("Recording-only preference")
    }

    private fun TrackPreferences.recordKeys(trackerIds: List<Long>) {
        trackerIds.forEach { trackerId ->
            trackUsername(trackerId)
            trackDisplayUsername(trackerId)
            trackPassword(trackerId)
            trackAuthExpired(trackerId)
            trackToken(trackerId)
            oauthState(trackerId)
            oauthCodeVerifier(trackerId)
        }
        mangabakaScoreType
        autoUpdateTrack
        autoUpdateTrackOnMarkRead
    }
}
