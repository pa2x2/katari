package mihon.entry.interactions.book.document.resource

import mihon.entry.interactions.book.preparation.BookRemoteResourceAuthorization
import mihon.entry.interactions.book.preparation.BookRemoteResourceRequest

/** Applies remembered publication-origin consent before presenting only newly requested remote resources. */
internal class BookRemoteResourceConsentCoordinator(
    private val preferences: BookRemoteResourceConsentPreferences,
) {
    fun pendingConsent(
        publicationId: String,
        authorization: BookRemoteResourceAuthorization,
        onResolved: () -> Unit,
    ): PendingBookRemoteResourceConsent? {
        if (authorization.remoteResourceRequests.isEmpty()) return null
        val approved = preferences.approvedOrigins(publicationId)
        val requestedOrigins = authorization.remoteResourceRequests.mapTo(linkedSetOf()) { it.origin }
        val alreadyApprovedOrigins = approved intersect requestedOrigins
        authorization.authorizeRemoteOrigins(alreadyApprovedOrigins)
        val pendingRequests = authorization.remoteResourceRequests.filterTo(linkedSetOf()) {
            it.origin !in approved
        }
        if (pendingRequests.isEmpty()) return null
        return PendingBookRemoteResourceConsent(
            publicationId = publicationId,
            authorization = authorization,
            requests = pendingRequests,
            alreadyApprovedOrigins = alreadyApprovedOrigins,
            onResolved = onResolved,
        )
    }

    fun resolve(pending: PendingBookRemoteResourceConsent, allow: Boolean) {
        val newlyApproved = if (allow) pending.requests.mapTo(linkedSetOf()) { it.origin } else emptySet()
        if (newlyApproved.isNotEmpty()) preferences.approve(pending.publicationId, newlyApproved)
        pending.authorization.authorizeRemoteOrigins(pending.alreadyApprovedOrigins + newlyApproved)
        pending.onResolved()
    }
}

internal data class PendingBookRemoteResourceConsent(
    val publicationId: String,
    val authorization: BookRemoteResourceAuthorization,
    val requests: Set<BookRemoteResourceRequest>,
    val alreadyApprovedOrigins: Set<String>,
    val onResolved: () -> Unit,
)
