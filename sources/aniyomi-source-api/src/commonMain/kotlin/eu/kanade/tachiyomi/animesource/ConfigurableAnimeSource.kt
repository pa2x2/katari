package eu.kanade.tachiyomi.animesource

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ConfigurableAnimeSource : AnimeSource {

    /**
     * Extensions open `source_<their id>` through the injected [Application] themselves, so this must name the same
     * file and go through the same call, which is where Katari applies profiles.
     */
    fun getSourcePreferences(): SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", Context.MODE_PRIVATE)

    fun setupPreferenceScreen(screen: PreferenceScreen)
}
