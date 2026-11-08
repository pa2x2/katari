package app.katari.desktop.webview

import app.katari.core.common.web.ChallengePage
import app.katari.core.common.web.ChallengePageEvents
import okhttp3.HttpUrl

/**
 * Without JCEF in the Java runtime the app runs on, desktop has no pages to solve challenges in, so a challenge goes
 * back to the source unsolved and the user is told it needs a web view.
 */
class NoChallengePage : ChallengePage {
    override val isOffered: Boolean = false

    override fun open(
        url: HttpUrl,
        headers: Map<String, String>,
        userAgent: String,
        events: ChallengePageEvents,
    ): ChallengePage.Session = error("Challenge pages need a Java runtime with JCEF")
}
