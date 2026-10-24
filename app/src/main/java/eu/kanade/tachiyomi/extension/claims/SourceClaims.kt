package eu.kanade.tachiyomi.extension.claims

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.model.Extension
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.identity.SourceProvider

/**
 * Resolves the loaded extensions' claims to their sources against the registry, as [resolveClaims] decides them, and
 * records the outcome there so it holds after a restart.
 */
@Inject
@SingleIn(AppScope::class)
class SourceClaims(
    private val identities: SourceIdentityRepository,
) {

    suspend fun resolve(extensions: Collection<Extension.Loaded>): ClaimResolution {
        val registered = identities.findAll()
        val missing = identitiesToRegister(extensions, registered)
        missing.forEach { identities.register(it) }
        val resolution = resolveClaims(extensions, if (missing.isEmpty()) registered else identities.findAll())
        resolution.newProviders.forEach { (key, provider) -> identities.recordProvider(key, provider) }
        return resolution
    }

    /**
     * Makes [extension] the provider of the source under [sourceKey] from the next [resolve] on, including after a
     * restart. The key stays, so the source keeps its library, history and settings.
     */
    suspend fun choose(extension: Extension.Loaded, sourceKey: Long) {
        require(extension.sources.any { it.id == sourceKey }) { "${extension.pkgName} has no source $sourceKey" }
        identities.recordProvider(sourceKey, SourceProvider(extension.providerKind, extension.pkgName))
    }
}
