package app.katari.gradle.chromium

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when the Chromium pinned for the app to download isn't the one of the pinned JetBrains Runtime's JCEF, or when
 * the licences screen names other versions of it. The app's JCEF talks to Chromium's server in a protocol of that JCEF
 * build's own, so a Chromium of another build is one the app can't run.
 */
abstract class CheckChromiumVersionTask : DefaultTask() {

    /** The runtime's `release` file, which names its JCEF's version and commit. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeRelease: RegularFileProperty

    @get:Input
    abstract val pinned: Property<String>

    /** The folder of the licences screen's library definitions. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val licenceLibraries: DirectoryProperty

    @TaskAction
    fun check() {
        val release = JcefRelease(runtimeRelease.get().asFile)
        val runtime = release.pin
        if (runtime != pinned.get()) {
            throw GradleException(
                "The JetBrains Runtime carries Chromium $runtime, but gradle/katari.versions.toml pins " +
                    "${pinned.get()}. Pin the runtime's, with the checksum and size " +
                    ":desktop:assembleChromiumArchive prints, and publish the archive with " +
                    "tools/chromium/publish-release.sh.",
            )
        }
        val outdated = ChromiumLicenceEntries(licenceLibraries.get().asFile).outdated(release)
        if (outdated.isNotEmpty()) {
            throw GradleException(
                "The licences screen names other versions than the JetBrains Runtime's Chromium has, in " +
                    "${outdated.joinToString()}. :desktop:assembleChromiumArchive writes them.",
            )
        }
    }
}
