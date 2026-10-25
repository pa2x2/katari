package eu.kanade.tachiyomi.extension.claims

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.SourceConflict
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.identity.RegisteredSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceProvider

class ClaimResolution(
    val loaded: Map<String, Extension.Loaded>,
    /** Extensions whose every source another one provides. */
    val conflicted: Map<String, Extension.NotLoaded>,
    /** The providers the registry doesn't record yet, by local key. */
    val newProviders: Map<Long, SourceProvider>,
)

/**
 * The identities of [extensions]' sources that need a local key before their claims can be resolved: those of schemes
 * that keep their IDs as keys and aren't [registered] yet. A scheme that allocates keys registers its identities while
 * loading, since its sources report the key as their ID.
 */
internal fun identitiesToRegister(
    extensions: Collection<Extension.Loaded>,
    registered: Collection<RegisteredSource>,
): Set<SourceIdentity> {
    val known = registered.mapTo(HashSet()) { it.identity }
    return extensions
        .filter { it.sourceIdScheme.externalIdIsLocalKey }
        .flatMap { extension -> extension.sources.map { SourceIdentity(extension.sourceIdScheme, it.id) } }
        .filterNotTo(LinkedHashSet()) { it in known }
}

/**
 * Decides which loaded extension provides each source, given every identity the registry holds. Two extensions can
 * claim the same identity, such as two packages of one Keiyoushi source, and in one scheme that means the same source,
 * so only one of them may register it. The extension the registry says was already providing it stays active, whether
 * it got there first or the user chose it; with none recorded, the package name decides, so the choice doesn't depend
 * on the order extensions happened to load in. The other is told who provides the source instead.
 *
 * A source registered in the app reports its local key as its ID. In a scheme that keeps its IDs as keys, a source
 * whose identity was given another key, because its ID was already someone else's, can't be registered. In one that
 * allocates keys, the source already reports its key, and one that names no identity of its extension's scheme can't
 * be registered either.
 */
internal fun resolveClaims(
    extensions: Collection<Extension.Loaded>,
    registered: Collection<RegisteredSource>,
): ClaimResolution {
    val keys = registered.associate { it.identity to it.localKey }
    val byKey = registered.associateBy { it.localKey }
    val providers = registered.associate { it.localKey to it.provider }

    fun keyOf(scheme: SourceIdScheme, sourceId: Long): Long? {
        return if (scheme.externalIdIsLocalKey) {
            keys[SourceIdentity(scheme, sourceId)]
        } else {
            sourceId.takeIf { byKey[it]?.identity?.scheme == scheme }
        }
    }

    val claims = extensions.flatMap { extension ->
        extension.sources.map { source -> Claim(extension, source, keyOf(extension.sourceIdScheme, source.id)) }
    }
    val (bindable, unbindable) = claims.partition { it.key == it.source.id }

    val winners = bindable
        .groupBy { it.source.id }
        .mapValues { (key, claimants) ->
            val provider = providers[key]
            claimants.firstOrNull { it.extension.pkgName == provider?.packageName }
                ?: claimants.minBy { it.extension.pkgName }
        }
    val newProviders = winners
        .mapValues { (_, winner) -> SourceProvider(winner.extension.providerKind, winner.extension.pkgName) }
        .filter { (key, provider) -> providers[key] != provider }

    val conflicts = bindable
        .filter { winners[it.source.id] !== it }
        .map { it.toConflict(providedBy = winners.getValue(it.source.id).extension.name, sourceKey = it.source.id) } +
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
    return ClaimResolution(loaded, conflicted, newProviders)
}

private class Claim(
    val extension: Extension.Loaded,
    val source: UnifiedSource,
    /** Null when no identity of the extension's scheme has a key for the source. */
    val key: Long?,
) {
    fun toConflict(providedBy: String?, sourceKey: Long?) = this to SourceConflict(
        sourceName = source.name,
        sourceLang = (source as? EntryCatalogueSource)?.lang.orEmpty(),
        providedBy = providedBy,
        sourceKey = sourceKey,
    )
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
