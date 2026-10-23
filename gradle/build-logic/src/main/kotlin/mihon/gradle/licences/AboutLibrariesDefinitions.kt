package mihon.gradle.licences

import groovy.json.JsonOutput
import java.io.File

/**
 * Writes [LibmpvNotices] as AboutLibraries manual definitions: a config directory whose `libraries/` and `licenses/`
 * hold one JSON file per entry, which the plugin adds to the libraries it finds through POM metadata.
 *
 * Every component gets a licence entry of its own, even where it shares a text with another, because the entry also
 * carries what only that component has: the binaries it is built into. The release itself is one more entry, whose
 * text is the release's notice with the attributions it requires (such as FreeType's credit line).
 */
internal object AboutLibrariesDefinitions {

    fun write(notices: LibmpvNotices, group: String, configDir: File) {
        val libraries = configDir.resolve("libraries")
        val licences = configDir.resolve("licenses")
        configDir.deleteRecursively()
        libraries.mkdirs()
        licences.mkdirs()

        fun entry(slug: String, name: String, version: String, website: String, licenceName: String, text: String) {
            val licenceId = "libmpv-android-$slug"
            licences.resolve("$slug.json").writeText(
                JsonOutput.toJson(mapOf("hash" to licenceId, "name" to licenceName, "content" to text)),
            )
            libraries.resolve("$slug.json").writeText(
                JsonOutput.toJson(
                    mapOf(
                        "uniqueId" to "$group:$slug",
                        "artifactVersion" to version,
                        "name" to name,
                        "website" to website,
                        "licenses" to listOf(licenceId),
                    ),
                ),
            )
        }

        entry(
            slug = RELEASE_SLUG,
            name = RELEASE_SLUG,
            version = notices.version,
            website = notices.source,
            licenceName = NOTICE_TITLE,
            text = notices.notice,
        )
        val slugs = mutableSetOf(RELEASE_SLUG)
        notices.components.forEach { component ->
            val slug = component.name.lowercase().replace(NON_SLUG, "-").trim('-')
            check(slugs.add(slug)) { "Two libmpv components are both named like $slug" }
            entry(
                slug = slug,
                name = component.name,
                version = component.version,
                website = component.website,
                licenceName = component.licence,
                text = buildString {
                    append("Built into: ").append(component.binaries).append("\n\n")
                    append("Complete corresponding source, with the build scripts: ").append(notices.source)
                    component.licenceTexts.forEach { append("\n\n").append(it.name).append("\n\n").append(it.text) }
                },
            )
        }
    }

    private const val RELEASE_SLUG = "libmpv-android"
    private const val NOTICE_TITLE = "Notices"
    private val NON_SLUG = Regex("[^a-z0-9]+")
}
