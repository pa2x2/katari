package app.katari.gradle.chromium

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails when the licences screen names other versions of Chromium, CEF or JCEF than the JetBrains Runtime carries. */
abstract class CheckChromiumLicencesTask : DefaultTask() {

    /** The runtime's `release` file, which names its JCEF's versions. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeRelease: RegularFileProperty

    /** The folder of the licences screen's library definitions. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val licenceLibraries: DirectoryProperty

    @TaskAction
    fun check() {
        val outdated = ChromiumLicenceEntries(licenceLibraries.get().asFile)
            .outdated(JcefRelease(runtimeRelease.get().asFile))
        if (outdated.isNotEmpty()) {
            throw GradleException(
                "The licences screen names other versions than the JetBrains Runtime's Chromium has, in " +
                    "${outdated.joinToString()}. :desktop:updateChromiumLicences writes them.",
            )
        }
    }
}
