package eu.kanade.tachiyomi.data.track

import dev.zacsweers.metro.Inject
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import java.security.MessageDigest

/**
 * The local keys of the Tachiyomi extension sources that self-hosted trackers such as Kavita and Suwayomi read their
 * server connection from. A tracker knows such a source only by the name, language and version its extension derives
 * the source's ID from, and that ID is the source's identity, not the key Katari stores it under.
 */
@Inject
class TrackerSourceKeys(
    private val identities: Lazy<SourceIdentityRepository>,
) {

    /** Null while the source's extension hasn't loaded. */
    suspend fun keyOf(name: String, lang: String, versionId: Int): Long? {
        val identity = SourceIdentity(SourceIdScheme.TACHIYOMI, tachiyomiSourceId(name, lang, versionId))
        return identities.value.find(identity)?.localKey
    }
}

/** The ID a Tachiyomi `HttpSource` generates for itself. */
private fun tachiyomiSourceId(name: String, lang: String, versionId: Int): Long {
    val key = "${name.lowercase()}/$lang/$versionId"
    val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
    return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
}
