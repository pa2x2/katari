package mihon.gradle.licences

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

/**
 * Lists the libmpv AAR's components on the licence screen. Its native libraries ship in the separately installed mpv
 * add-on, so no POM tells AboutLibraries about them; this reads the inventory the pinned AAR itself carries and
 * writes it as the plugin's manual definitions ([AboutLibrariesDefinitions]). It fails if a component lacks a licence
 * or its text.
 */
@CacheableTask
abstract class GenerateLibmpvLicencesTask : DefaultTask() {

    /** Exactly one AAR. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val aar: ConfigurableFileCollection

    /** The Maven group the entries' ids use, so they sort together and can't collide with a real artifact's. */
    @get:Input
    abstract val idGroup: Property<String>

    /** An AboutLibraries `configPath`. */
    @get:OutputDirectory
    abstract val configDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val archive = aar.singleFile
        val notices = ZipFile(archive).use { zip ->
            fun read(name: String): String {
                val entry =
                    zip.getEntry("$ASSETS/$name") ?: throw GradleException("${archive.name} has no $ASSETS/$name")
                return zip.getInputStream(entry).use { it.reader().readText() }
            }
            val loadOrder = JsonSlurper().parseText(read(LOAD_ORDER)) as? Map<*, *>
            val version = loadOrder?.get("version") as? String ?: throw GradleException("$LOAD_ORDER has no version")
            LibmpvNotices.parse(read(THIRD_PARTY), read(LICENCES), version)
        }
        AboutLibrariesDefinitions.write(notices, idGroup.get(), configDir.get().asFile)
    }

    private companion object {
        const val ASSETS = "assets/libmpv"
        const val THIRD_PARTY = "third-party.json"
        const val LICENCES = "THIRD_PARTY_LICENSES.txt"
        const val LOAD_ORDER = "load-order.json"
    }
}
