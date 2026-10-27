package mihon.feature.profiles.core

import android.content.Intent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** The intent the app was opened with, kept while a profile is picked or unlocked and handled once one is open. */
@Inject
@SingleIn(AppScope::class)
class ProfilePendingIntent(preferenceStore: PreferenceStore) {
    private val intentUri: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("pending_profile_intent_uri"),
        "",
    )

    fun store(intent: Intent?) {
        if (intent == null) {
            intentUri.delete()
            return
        }
        intentUri.set(intent.toUri(Intent.URI_INTENT_SCHEME))
    }

    fun consume(): Intent? {
        val uri = intentUri.get()
            .takeIf { it.isNotBlank() }
            ?: return null
        intentUri.delete()
        return runCatching {
            Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        }.getOrNull()
    }
}
