package mihon.entry.interactions.host.tracking

import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.ExternalLoginTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerCredentialIdentity
import eu.kanade.tachiyomi.data.track.TrackerLogin
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.service.SourceManager

internal class AppEntryTrackingAccountHost(
    private val trackerManager: TrackerManager,
    private val sourceManager: SourceManager,
) : EntryTrackingAccountHost {
    override fun currentAccounts(): List<EntryTrackingHostAccount> {
        return trackerManager.trackers
            .sortedBy(Tracker::accountOrder)
            .map { tracker -> tracker.toHostAccount() }
    }

    override fun observeAccounts(): Flow<List<EntryTrackingHostAccount>> {
        return combine(trackerManager.trackers.map(Tracker::isLoggedInFlow)) {
            currentAccounts()
        }
    }

    override fun storedCredentials(serviceId: Long): EntryTrackingHostStoredCredentials {
        val tracker = requireService(serviceId)
        return EntryTrackingHostStoredCredentials(tracker.getUsername(), tracker.getPassword())
    }

    override suspend fun beginExternalLogin(serviceId: Long): String {
        val tracker = requireService(serviceId) as? ExternalLoginTracker
            ?: error("Tracking service $serviceId does not use external login")
        return tracker.authorizationUri().toString()
    }

    override suspend fun loginWithCredentials(serviceId: Long, username: String, password: String) {
        val tracker = requireService(serviceId)
        check(tracker.accountLogin is TrackerLogin.Credentials) {
            "Tracking service $serviceId does not use credential login"
        }
        try {
            tracker.login(username, password)
        } catch (error: Throwable) {
            tracker.logout()
            throw error
        }
    }

    override suspend fun loginPassively(serviceId: Long) {
        val tracker = requireService(serviceId)
        val enhanced = tracker as? EnhancedTracker
            ?: error("Tracking service $serviceId does not use passive login")
        enhanced.loginNoop()
    }

    override suspend fun logout(serviceId: Long) {
        requireService(serviceId).logout()
    }

    private fun Tracker.toHostAccount(): EntryTrackingHostAccount {
        return EntryTrackingHostAccount(
            service = toHostService(),
            loginMethod = when (val login = accountLogin) {
                TrackerLogin.External -> EntryTrackingHostLoginMethod.External
                is TrackerLogin.Credentials -> EntryTrackingHostLoginMethod.Credentials(
                    when (login.identity) {
                        TrackerCredentialIdentity.USERNAME -> EntryTrackingHostCredentialIdentity.USERNAME
                        TrackerCredentialIdentity.EMAIL -> EntryTrackingHostCredentialIdentity.EMAIL
                    },
                )
                TrackerLogin.Passive -> EntryTrackingHostLoginMethod.Passive
            },
            isLoggedIn = isLoggedIn,
            displayUsername = getDisplayUsername(),
            isAvailable = isAvailable(),
        )
    }

    private fun Tracker.isAvailable(): Boolean {
        val enhanced = this as? EnhancedTracker ?: return true
        val acceptedSources = enhanced.getAcceptedSources()
        return sourceManager.getAll().any { it::class.qualifiedName in acceptedSources }
    }

    private fun requireService(serviceId: Long) = checkNotNull(trackerManager.get(serviceId)) {
        "Tracking service $serviceId is not registered"
    }
}
