package mihon.gradle.licences

import groovy.json.JsonSlurper
import org.gradle.api.GradleException

/**
 * The third-party inventory a libmpv-android release ships in its AAR: `third-party.json` lists the components and
 * names each one's licence texts, and `THIRD_PARTY_LICENSES.txt` holds the release's notice followed by those texts,
 * each under a `---- <name> ----` line.
 *
 * Only the names `third-party.json` refers to delimit texts: a licence text can contain a line of the same shape
 * (libc++'s has `---- LLVM Exceptions to the Apache 2.0 License ----`), which belongs to the text it is in.
 */
internal class LibmpvNotices(
    val version: String,
    /** The release page with the complete corresponding source and build scripts. */
    val source: String,
    /** Everything before the first licence text: the release's own notice and its component index. */
    val notice: String,
    val components: List<Component>,
) {

    class Component(
        val name: String,
        val version: String,
        /** An SPDX expression, possibly compound such as `MIT OR Apache-2.0 OR BSL-1.0`. */
        val licence: String,
        val website: String,
        /** Where the component ends up, such as `libavformat.so (static)`. */
        val binaries: String,
        val licenceTexts: List<LicenceText>,
    )

    class LicenceText(val name: String, val text: String)

    companion object {

        fun parse(thirdPartyJson: String, licencesText: String, version: String): LibmpvNotices {
            val inventory = JsonSlurper().parseText(thirdPartyJson) as? Map<*, *>
                ?: throw GradleException("third-party.json is not an object")
            val source = inventory.requireString("source", "third-party.json")
            val entries = inventory["components"] as? List<*>
            if (entries.isNullOrEmpty()) throw GradleException("third-party.json lists no components")
            val referencedTexts = entries.map { entry ->
                val component =
                    entry as? Map<*, *> ?: throw GradleException("third-party.json has a non-object component")
                val name = component.requireString("name", "a third-party.json component")
                val texts = (component["licenseTexts"] as? List<*>)?.map { it.toString() }
                if (texts.isNullOrEmpty()) throw GradleException("$name names no licence text in third-party.json")
                Triple(component, name, texts)
            }
            val textsByName = splitTexts(licencesText, referencedTexts.flatMap { it.third }.toSet())

            val firstText = textsByName.values.minOf { it.first }
            val notice = licencesText.lineSequence().take(firstText).joinToString("\n").trim()
            if (source !in notice) {
                throw GradleException("THIRD_PARTY_LICENSES.txt's notice doesn't point to the source at $source")
            }

            val components = referencedTexts.map { (component, name, texts) ->
                Component(
                    name = name,
                    version = component.requireString("version", name),
                    licence = component.requireString("license", name),
                    website = component.requireString("url", name),
                    binaries = component.requireString("binaries", name),
                    licenceTexts = texts.map { LicenceText(it, textsByName.getValue(it).second) },
                )
            }
            return LibmpvNotices(version, source, notice, components)
        }

        /** Maps each of [names] to the line its header is on and the text under it. */
        private fun splitTexts(licencesText: String, names: Set<String>): Map<String, Pair<Int, String>> {
            val lines = licencesText.lines()
            val headers = lines.withIndex()
                .mapNotNull { (index, line) -> line.headerName()?.takeIf { it in names }?.let { it to index } }
            headers.groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }.keys.firstOrNull()?.let {
                throw GradleException("THIRD_PARTY_LICENSES.txt has the licence text $it more than once")
            }
            names.firstOrNull { name -> headers.none { it.first == name } }?.let {
                throw GradleException("THIRD_PARTY_LICENSES.txt has no licence text $it")
            }
            return headers.withIndex().associate { (position, header) ->
                val (name, start) = header
                val end = headers.getOrNull(position + 1)?.second ?: lines.size
                val text = lines.subList(start + 1, end).joinToString("\n").trim()
                if (text.isEmpty()) throw GradleException("THIRD_PARTY_LICENSES.txt's licence text $name is empty")
                name to (start to text)
            }
        }

        private fun String.headerName(): String? = takeIf { it.startsWith(HEADER_MARK) && it.endsWith(HEADER_MARK) }
            ?.removePrefix(HEADER_MARK)
            ?.removeSuffix(HEADER_MARK)
            ?.trim()

        private fun Map<*, *>.requireString(key: String, owner: String): String =
            (this[key] as? String)?.takeIf { it.isNotBlank() } ?: throw GradleException("$owner has no $key")

        private const val HEADER_MARK = "----"
    }
}
