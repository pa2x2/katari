package app.katari.gradle.jetbrainsruntime

import app.katari.gradle.configurations.VERIFY_TESTS_TASK_NAME
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.zip.ZipFile

/**
 * Fails when the JCEF API this module compiles against isn't the one the pinned JetBrains Runtime carries, which would
 * let code compile against calls the runtime doesn't have. JetBrains publishes the API for a JCEF build only now and
 * then, so the API may come from an earlier build of the same JCEF: the check compares everything a JCEF version names
 * up to its API version (CEF and its commit, Chromium, the API) and leaves out the branch and build number after it.
 */
abstract class JcefApiVersionCheckTask : DefaultTask() {

    /** The runtime's `release` file, whose `JCEF_VERSION_DETAILED` names the JCEF it carries. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeRelease: RegularFileProperty

    /** The JCEF API jar, whose `com/jetbrains/cef/version.info` names the JCEF it was built from. */
    @get:Classpath
    abstract val api: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val runtime = runtimeRelease.get().asFile.readLines()
            .firstNotNullOfOrNull { line -> line.removePrefix("$RELEASE_KEY=").takeIf { it != line } }
            ?.trim('"')
            ?: throw GradleException("The JetBrains Runtime's release file names no $RELEASE_KEY")
        val apiVersion = api.files.firstNotNullOfOrNull(::versionInfo)
            ?: throw GradleException("No JCEF API jar with $VERSION_INFO among ${api.files}")
        if (apiRelease(runtime) != apiRelease(apiVersion)) {
            throw GradleException(
                "The JetBrains Runtime carries JCEF $runtime, but the JCEF API in gradle/libs.versions.toml is " +
                    "$apiVersion. Pin the API JetBrains publishes for the runtime's JCEF, or a runtime with the API's.",
            )
        }
    }

    private fun versionInfo(jar: File): String? = ZipFile(jar).use { zip ->
        zip.getEntry(VERSION_INFO)?.let { entry -> zip.getInputStream(entry).bufferedReader().use { it.readLine() } }
    }?.trim()

    private fun apiRelease(version: String): String = API_RELEASE.matchEntire(version)?.groupValues?.get(1)
        ?: throw GradleException("Can't read the JCEF version $version")

    private companion object {
        const val RELEASE_KEY = "JCEF_VERSION_DETAILED"
        const val VERSION_INFO = "com/jetbrains/cef/version.info"

        // 150.0.14-g7c1aa68-chromium-150.0.7871.129-api-1.21-263-b17: everything up to the API version, then the
        // branch and build number.
        val API_RELEASE = Regex("""(.+-api-[\d.]+)-\d+-b\d+""")
    }
}

/**
 * Registers `checkJcefApiVersion` over this module's compile-time JCEF API, run by `verifyTests`, the check every change
 * passes before it is committed.
 */
fun Project.checkJcefApiMatchesJetBrainsRuntime() {
    val runtime = jetBrainsRuntime()
    val provision = provisionJetBrainsRuntime(runtime)
    val api = configurations.named("compileClasspath").map { classpath ->
        classpath.incoming.artifactView {
            componentFilter { it is ModuleComponentIdentifier && it.group == JCEF_GROUP }
        }.files
    }
    val check = tasks.register<JcefApiVersionCheckTask>("checkJcefApiVersion") {
        description = "Checks that the JCEF API compiled against is the one the JetBrains Runtime carries."
        group = "verification"
        dependsOn(provision)
        runtimeRelease.set(runtime.release)
        this.api.from(api)
    }
    tasks.named(VERIFY_TESTS_TASK_NAME) { dependsOn(check) }
}

private const val JCEF_GROUP = "org.jetbrains.intellij.deps.jcef"
