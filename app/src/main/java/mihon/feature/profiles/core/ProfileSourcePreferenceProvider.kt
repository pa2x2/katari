package mihon.feature.profiles.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider

class ProfileSourcePreferenceProvider(
    private val application: Application,
    private val profileStore: ProfileStore,
) : EntryPreferenceProvider {

    override fun key(sourceId: Long): String {
        return profileStore.sourcePreferenceKey(sourceId)
    }

    override fun preferences(sourceId: Long): SharedPreferences {
        return application.getSharedPreferences(key(sourceId), Context.MODE_PRIVATE)
    }
}
