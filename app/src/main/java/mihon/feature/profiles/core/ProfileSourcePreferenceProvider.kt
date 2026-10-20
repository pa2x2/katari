package mihon.feature.profiles.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import tachiyomi.core.common.preference.SourcePreferenceProfile

/** Gives extensions the preferences of the profile their call works for; see [SourcePreferenceProfile]. */
class ProfileSourcePreferenceProvider(
    private val application: Application,
    private val profileStore: ProfileStore,
) : EntryPreferenceProvider {

    override fun key(sourceId: Long): String {
        return profileStore.sourcePreferenceKey(
            sourceId,
            SourcePreferenceProfile.current ?: profileStore.currentProfileId,
        )
    }

    override fun preferences(sourceId: Long): SharedPreferences {
        return application.getSharedPreferences(key(sourceId), Context.MODE_PRIVATE)
    }
}
