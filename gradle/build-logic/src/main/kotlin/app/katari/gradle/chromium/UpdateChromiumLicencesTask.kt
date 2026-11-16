package app.katari.gradle.chromium

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Writes the versions of Chromium, CEF and JCEF the JetBrains Runtime carries into the licences screen's entries, which
 * are committed: run after the runtime is upgraded.
 */
abstract class UpdateChromiumLicencesTask : DefaultTask() {

    @get:Internal
    abstract val runtimeRelease: RegularFileProperty

    @get:Internal
    abstract val licenceLibraries: DirectoryProperty

    @TaskAction
    fun update() {
        ChromiumLicenceEntries(licenceLibraries.get().asFile).write(JcefRelease(runtimeRelease.get().asFile))
    }
}
