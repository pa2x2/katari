package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.model.Extension
import tachiyomi.domain.source.identity.RegisteredSource
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.model.StubSource

/**
 * The sources the stores list, by the key Katari stores each under. A listing names its sources by the IDs of its
 * ecosystem, and in one that allocates keys, such as Aniyomi's, the same ID can name a Tachiyomi source too. Such a
 * source only has a key once an extension providing it was loaded; until then it's left out, since listing a source
 * doesn't register it.
 */
internal fun listedSourcesByKey(
    extensions: Collection<Extension.Available>,
    registered: Collection<RegisteredSource>,
): Map<Long, StubSource> {
    val keys = registered.associate { it.identity to it.localKey }
    return extensions
        .flatMap { extension ->
            extension.sources.mapNotNull { source ->
                val identity = SourceIdentity(extension.sourceIdScheme, source.id)
                val key = keys[identity] ?: source.id.takeIf { extension.sourceIdScheme.externalIdIsLocalKey }
                key?.let { source.toStubSource().copy(id = it) }
            }
        }
        .associateBy { it.id }
}
