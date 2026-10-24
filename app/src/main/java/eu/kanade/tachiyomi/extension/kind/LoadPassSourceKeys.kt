package eu.kanade.tachiyomi.extension.kind

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local keys for the sources one load pass creates, for kinds whose sources report a key Katari gave them rather than
 * their own ID. The registry is read once however many extensions ask, and only when one does; an identity it doesn't
 * hold yet is registered.
 *
 * Every extension loads at once, so known keys are read without a lock and only a registration takes one. Taking it
 * for every lookup made hundreds of sources wait their turn on a CPU busy loading classes, which at startup with
 * Yūzōnō's 250 extensions cost about a second.
 */
internal class LoadPassSourceKeys(private val identities: SourceIdentityRepository) {

    private val readStarted = AtomicBoolean(false)
    private val known = CompletableDeferred<ConcurrentHashMap<SourceIdentity, Long>>()
    private val registering = Mutex()

    suspend fun keyOf(identity: SourceIdentity): Long {
        if (readStarted.compareAndSet(false, true)) {
            try {
                known.complete(identities.findAll().associateTo(ConcurrentHashMap()) { it.identity to it.localKey })
            } catch (e: Throwable) {
                known.completeExceptionally(e)
                throw e
            }
        }
        val keys = known.await()
        keys[identity]?.let { return it }
        return registering.withLock {
            keys[identity] ?: identities.register(identity).also { keys[identity] = it }
        }
    }
}
