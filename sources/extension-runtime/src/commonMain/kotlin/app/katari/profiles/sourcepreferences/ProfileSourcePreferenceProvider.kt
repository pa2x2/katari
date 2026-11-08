package app.katari.profiles.sourcepreferences

import android.content.SharedPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider

/** Gives sources the settings of the profile each call works for; see [SourcePreferenceFiles]. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProfileSourcePreferenceProvider(
    private val files: SourcePreferenceFiles,
) : EntryPreferenceProvider {

    // A source that opens its file by this name itself, on the Application it's given, is routed back to forSource
    override fun key(sourceId: Long): String = extensionPreferenceFileName(sourceId)

    override fun preferences(sourceId: Long): SharedPreferences = files.forSource(sourceId)
}
