package eu.kanade.tachiyomi.extension.kind

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository

/**
 * Local keys for the sources one load pass creates, for kinds whose sources report a key Katari gave them rather than
 * their own ID. The registry is read once however many extensions ask, and only when one does; an identity it doesn't
 * hold yet is registered.
 */
internal class LoadPassSourceKeys(private val identities: SourceIdentityRepository) {

    private val mutex = Mutex()
    private var keys: MutableMap<SourceIdentity, Long>? = null

    suspend fun keyOf(identity: SourceIdentity): Long = mutex.withLock {
        val known = keys
            ?: identities.findAll().associateTo(HashMap()) { it.identity to it.localKey }.also { keys = it }
        known.getOrPut(identity) { identities.register(identity) }
    }
}
