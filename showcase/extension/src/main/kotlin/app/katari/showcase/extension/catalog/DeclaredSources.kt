package app.katari.showcase.extension.catalog

/**
 * The sources in showcase/content/source-list/sources.json, which the build packages as a Java resource.
 *
 * Read through this class's own loader: the extension loader resolves its own resources before Katari's.
 */
internal fun declaredSources(): List<ShowcaseSourceDefinition> {
    val resource = ShowcaseSourceDefinition::class.java.classLoader!!.getResourceAsStream("sources.json")
        ?: error("The showcase extension was built without its source list")
    val text = resource.bufferedReader().use { it.readText() }
    return showcaseJson.decodeFromString<List<ShowcaseSourceDefinition>>(text)
}
