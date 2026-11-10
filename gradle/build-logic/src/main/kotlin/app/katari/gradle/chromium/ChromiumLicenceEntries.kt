package app.katari.gradle.chromium

import java.io.File

/**
 * The entries of the desktop app's licences screen for what the Chromium download is made of, in the folder of library
 * definitions AboutLibraries reads. Each names the version of its project, which only the runtime's release knows.
 */
internal class ChromiumLicenceEntries(private val libraries: File) {

    /** The entries whose version isn't [release]'s, by file name. */
    fun outdated(release: JcefRelease): List<String> = versions(release)
        .filter { (name, version) -> VERSION.find(file(name).readText())?.groupValues?.get(1) != version }
        .keys.toList()

    fun write(release: JcefRelease) {
        versions(release).forEach { (name, version) ->
            val file = file(name)
            file.writeText(file.readText().replace(VERSION, "\"artifactVersion\": \"$version\""))
        }
    }

    private fun versions(release: JcefRelease) = mapOf(
        "chromium-desktop-chromium.json" to release.chromiumVersion,
        "chromium-desktop-cef.json" to release.cefVersion,
        "chromium-desktop-jcef.json" to release.jcefVersion,
    )

    private fun file(name: String) = libraries.resolve(name)

    private companion object {
        val VERSION = Regex("\"artifactVersion\": \"([^\"]*)\"")
    }
}
