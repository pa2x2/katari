package tachiyomi.domain.source.identity

/**
 * The registry of source identities and the local keys every other table and preference stores for them. A key once
 * given to an identity stays with it.
 */
interface SourceIdentityRepository {

    /** The local key of [identity], registering it first if it's new. */
    suspend fun register(identity: SourceIdentity): Long

    suspend fun find(localKey: Long): RegisteredSource?

    suspend fun findAll(): List<RegisteredSource>

    suspend fun recordProvider(localKey: Long, provider: SourceProvider)
}

data class RegisteredSource(
    val localKey: Long,
    val identity: SourceIdentity,
    /** Null until an extension provided the source since identities were recorded. */
    val provider: SourceProvider?,
)
