package eu.kanade.tachiyomi.data.track

sealed interface TrackerLogin {
    data object External : TrackerLogin

    data class Credentials(
        val identity: TrackerCredentialIdentity,
    ) : TrackerLogin

    data object Passive : TrackerLogin
}

enum class TrackerCredentialIdentity {
    USERNAME,
    EMAIL,
}

interface ExternalLoginTracker : Tracker {
    override val accountLogin: TrackerLogin
        get() = TrackerLogin.External

    /** The service's login page, which sends the result to the tracker's redirect address once the user logs in. */
    fun authorizationUri(): String
}
