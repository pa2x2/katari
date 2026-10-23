package mihon.feature.profiles.sourcepreferences

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider

/** Gives sources the settings of the profile each call works for; see [SourcePreferenceFiles]. */
class ProfileSourcePreferenceProvider(
    private val files: SourcePreferenceFiles,
) : EntryPreferenceProvider {

    // A source that opens its file by this name itself is routed back to forSource by the App
    override fun key(sourceId: Long): String = extensionPreferenceFileName(sourceId)

    override fun preferences(sourceId: Long): SharedPreferences = files.forSource(sourceId)
}
