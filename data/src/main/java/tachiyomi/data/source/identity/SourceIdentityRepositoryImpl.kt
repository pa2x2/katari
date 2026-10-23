package tachiyomi.data.source.identity

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.identity.RegisteredSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.identity.SourceProvider
import tachiyomi.domain.source.identity.SourceProviderKind

/** How keys are given out is documented in `source_identities.sq`. */
@Inject
@SingleIn(AppScope::class)
class SourceIdentityRepositoryImpl(
    private val handler: DatabaseHandler,
) : SourceIdentityRepository {

    override suspend fun register(identity: SourceIdentity): Long {
        return handler.await(inTransaction = true) {
            val queries = source_identitiesQueries
            val scheme = identity.scheme.key
            queries.findByIdentity(scheme, identity.externalId, ::mapRegisteredSource).awaitAsOneOrNull()
                ?.let { return@await it.localKey }

            if (identity.scheme.externalIdIsLocalKey) {
                queries.insertKeepingExternalId(externalId = identity.externalId, idScheme = scheme)
                queries.findByIdentity(scheme, identity.externalId, ::mapRegisteredSource).awaitAsOneOrNull()
                    ?.let { return@await it.localKey }
            }

            queries.insertAllocatingKey(idScheme = scheme, externalId = identity.externalId)
            queries.findByIdentity(scheme, identity.externalId, ::mapRegisteredSource).awaitAsOne().localKey
        }
    }

    override suspend fun find(localKey: Long): RegisteredSource? {
        return handler.awaitOneOrNull { source_identitiesQueries.findByLocalKey(localKey, ::mapRegisteredSource) }
    }

    override suspend fun findAll(): List<RegisteredSource> {
        return handler.awaitList { source_identitiesQueries.findAll(::mapRegisteredSource) }
    }

    override suspend fun recordProvider(localKey: Long, provider: SourceProvider) {
        handler.await {
            source_identitiesQueries.updateProvider(
                providerKind = provider.kind.key,
                providerPackage = provider.packageName,
                localKey = localKey,
            )
        }
    }
}

private fun mapRegisteredSource(
    localKey: Long,
    idScheme: String,
    externalId: Long,
    providerKind: String?,
    providerPackage: String?,
) = RegisteredSource(
    localKey = localKey,
    identity = SourceIdentity(SourceIdScheme(idScheme), externalId),
    provider = providerKind?.let { SourceProvider(SourceProviderKind(it), providerPackage) },
)
