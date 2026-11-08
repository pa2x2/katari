package app.katari.core.common.web

/**
 * What a [ChallengePage] tells about the page it loads, on any thread, in the order the main frame's loads happen: a
 * load's HTTP error comes before its finish, and nothing of a later load before either.
 */
interface ChallengePageEvents {
    /** The main frame finished loading [url]; the cookies the page has set are in the app's cookie jar by now. */
    fun onPageFinished(url: String)

    /** The main frame got an HTTP error, a status of 400 or above, with the response's [headers]. */
    fun onHttpError(statusCode: Int, headers: Map<String, String>)

    /** The challenge in the main frame posted [event], such as `interactiveBegin` or `fail`. */
    fun onChallengeEvent(event: String)

    /** The page can't go on, as when its renderer ended; nothing more comes from it. */
    fun onPageGone()
}
