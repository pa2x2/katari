package eu.kanade.tachiyomi.data.track

import android.net.Uri

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

    fun authorizationUri(): Uri
}
