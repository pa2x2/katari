package eu.kanade.tachiyomi.ui.main

import app.katari.entry.interactions.media.EntryMediaCacheClearResult
import app.katari.entry.interactions.media.EntryMediaCacheFeature
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.IncognitoPreferences
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat

/** What every launch of the app resets once its profile is open: incognito mode, and the caches set to clear. */
@Inject
class LaunchCleanup(
    private val incognitoPreferences: IncognitoPreferences,
    private val mediaCacheFeature: EntryMediaCacheFeature,
) {
    fun run(scope: CoroutineScope) {
        incognitoPreferences.incognitoMode.set(false)

        scope.launchIO {
            mediaCacheFeature.clearEnabledOnLaunch().forEach { result ->
                if (result is EntryMediaCacheClearResult.Failed) {
                    logcat(LogPriority.ERROR, result.error)
                }
            }
        }
    }
}
