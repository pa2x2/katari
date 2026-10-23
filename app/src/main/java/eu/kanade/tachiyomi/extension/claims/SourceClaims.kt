package eu.kanade.tachiyomi.extension.claims

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.SourceConflict
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.identity.SourceProvider

/**
 * Decides which loaded extension provides each source. Two extensions can claim the same identity, such as two
 * packages of one Keiyoushi source, and in the tachiyomi scheme that means the same source, so only one of them may
 * register it. The extension the registry says was already providing it stays active, whether it got there first or the
 * user chose it with [choose]; with none recorded, the package name decides, so the choice doesn't depend on the order
 * extensions happened to load in. The other is told who provides the source instead.
 *
 * A source registered in the app reports its local key as its ID. Every scheme loaded so far keeps the ID as the key,
 * so a source whose identity was given another key, because its ID was already someone else's, can't be registered.
 */
@Inject
@SingleIn(AppScope::class)
class SourceClaims(
    private val identities: SourceIdentityRepository,
) {

    class Resolution(
        val loaded: Map<String, Extension.Loaded>,
        /** Extensions whose every source another one provides. */
        val conflicted: Map<String, Extension.NotLoaded>,
    )

    suspend fun resolve(extensions: Collection<Extension.Loaded>): Resolution {
        val registered = identities.findAll()
        val keys = registered.associate { it.identity to it.localKey }.toMutableMap()
        val providers = registered.associate { it.localKey to it.provider }

        val claims = extensions.flatMap { extension ->
            extension.sources.map { source ->
                val identity = SourceIdentity(extension.sourceIdScheme, source.id)
                val key = keys.getOrPut(identity) { identities.register(identity) }
                Claim(extension, source, key)
            }
        }
        val (bindable, unbindable) = claims.partition { it.key == it.source.id }

        val winners = bindable
            .groupBy { it.key }
            .mapValues { (key, claimants) ->
                val provider = providers[key]
                claimants.firstOrNull { it.extension.pkgName == provider?.packageName }
                    ?: claimants.minBy { it.extension.pkgName }
            }
        winners.forEach { (key, winner) ->
            val provider = SourceProvider(winner.extension.providerKind, winner.extension.pkgName)
            if (providers[key] != provider) identities.recordProvider(key, provider)
        }

        val conflicts = bindable
            .filter { winners[it.key] !== it }
            .map { it.toConflict(providedBy = winners.getValue(it.key).extension.name, sourceKey = it.key) } +
            unbindable.map { it.toConflict(providedBy = winners[it.source.id]?.extension?.name, sourceKey = null) }

        val loaded = mutableMapOf<String, Extension.Loaded>()
        val conflicted = mutableMapOf<String, Extension.NotLoaded>()
        extensions.forEach { extension ->
            val lost = conflicts.filter { (claim, _) ->
                claim.extension === extension
            }.map { (_, conflict) -> conflict }
            val provided = extension.sources.filter { source -> winners[source.id]?.source === source }
            if (provided.isEmpty() && lost.isNotEmpty()) {
                conflicted[extension.pkgName] = extension.asConflicted(lost)
            } else {
                loaded[extension.pkgName] = extension.copy(sources = provided, sourceConflicts = lost)
            }
        }
        return Resolution(loaded, conflicted)
    }

    /**
     * Makes [extension] the provider of the source under [sourceKey] from the next [resolve] on, including after a
     * restart. The key stays, so the source keeps its library, history and settings.
     */
    suspend fun choose(extension: Extension.Loaded, sourceKey: Long) {
        require(extension.sources.any { it.id == sourceKey }) { "${extension.pkgName} has no source $sourceKey" }
        identities.recordProvider(sourceKey, SourceProvider(extension.providerKind, extension.pkgName))
    }

    private class Claim(
        val extension: Extension.Loaded,
        val source: UnifiedSource,
        val key: Long,
    ) {
        fun toConflict(providedBy: String?, sourceKey: Long?) = this to SourceConflict(
            sourceName = source.name,
            sourceLang = (source as? EntryCatalogueSource)?.lang.orEmpty(),
            providedBy = providedBy,
            sourceKey = sourceKey,
        )
    }
}

private fun Extension.Loaded.asConflicted(conflicts: List<SourceConflict>) = Extension.NotLoaded(
    name = name,
    pkgName = pkgName,
    versionName = versionName,
    versionCode = versionCode,
    isShared = isShared,
    contentWarning = contentWarning,
    signatures = signatures,
    libVersion = libVersion,
    lang = lang,
    hasUpdate = hasUpdate,
    store = store,
    reason = Extension.NotLoaded.Reason.SourceConflicts(conflicts),
)
