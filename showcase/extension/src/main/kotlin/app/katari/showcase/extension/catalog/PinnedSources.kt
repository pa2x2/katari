package app.katari.showcase.extension.catalog

import kotlinx.serialization.Serializable

@Serializable
private data class PinnedLock(val sources: List<ShowcaseSourceDefinition>)

/**
 * The sources declared in the pinned content lock, which the build packages as a Java resource.
 *
 * Read through this class's own loader: the extension loader resolves its own resources before Katari's.
 */
internal fun pinnedSources(): List<ShowcaseSourceDefinition> {
    val resource = PinnedLock::class.java.classLoader!!.getResourceAsStream("content.lock.json")
        ?: error("The showcase extension was built without its content lock")
    val text = resource.bufferedReader().use { it.readText() }
    return showcaseJson.decodeFromString<PinnedLock>(text).sources
}
