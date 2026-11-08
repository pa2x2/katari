package app.katari.entry.interactions.anime.introtimes

import app.katari.domain.app.AppIdentity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import tachiyomi.core.common.Constants
import kotlin.coroutines.cancellation.CancellationException

/** [block]'s answer from [service], or null when it couldn't be reached or answered with an error. */
internal inline fun <T> askOnline(service: String, block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    logcat("IntroTimes", LogPriority.WARN) { "Couldn't ask $service for intro times\n${e.asLog()}" }
    null
}

/**
 * How Katari names itself to Wikidata and TVmaze. Wikimedia's policy asks clients for a name and a way to reach their
 * makers, and the app's usual header poses as a browser.
 */
@Inject
@SingleIn(AppScope::class)
internal class IntroTimesUserAgent(appIdentity: AppIdentity) {
    val value = "Katari/v${appIdentity.versionName} (${Constants.URL_REPOSITORY})"
}
