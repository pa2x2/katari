package app.katari.gradle.chromium

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when the Chromium pinned for the app to download isn't the one of the pinned JetBrains Runtime's JCEF. The
 * app's JCEF talks to Chromium's server in a protocol of that JCEF build's own, so a Chromium of another build is one
 * the app can't run.
 */
abstract class CheckChromiumVersionTask : DefaultTask() {

    /** The runtime's `release` file, which names its JCEF's version and commit. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeRelease: RegularFileProperty

    @get:Input
    abstract val pinned: Property<String>

    @TaskAction
    fun check() {
        val release = runtimeRelease.get().asFile.readLines()
        fun value(key: String) = release
            .firstNotNullOfOrNull { line -> line.removePrefix("$key=").takeIf { it != line } }
            ?.trim('"')
            ?: throw GradleException("The JetBrains Runtime's release file names no $key")
        // 150.0.14.1233+g9df0879 and the commit it abbreviates
        val runtime = "${value(VERSION_KEY).substringBefore('+')}-${value(COMMIT_KEY).take(COMMIT_LENGTH)}"
        if (runtime != pinned.get()) {
            throw GradleException(
                "The JetBrains Runtime carries Chromium $runtime, but gradle/katari.versions.toml pins " +
                    "${pinned.get()}. Pin the runtime's, with the checksum and size " +
                    ":desktop:assembleChromiumArchive prints, and publish the archive with " +
                    "tools/chromium/publish-release.sh.",
            )
        }
    }

    private companion object {
        const val VERSION_KEY = "JCEF_VERSION"
        const val COMMIT_KEY = "JCEF_COMMIT_HASH"
        const val COMMIT_LENGTH = 12
    }
}
