package mihon.gradle.extensionabi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject

/**
 * Fails when the classes in [currentJars] break binary compatibility with a pinned extension-lib baseline, so
 * extensions already compiled against that lib keep linking.
 *
 * The baseline is stored Base64-encoded and checked against [expectedSha256], so a replaced baseline can't silently
 * relax the check. japicmp runs in an isolated class loader because its javassist must not meet the build's.
 */
@CacheableTask
abstract class ExtensionAbiCheckTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val encodedBaseline: RegularFileProperty

    @get:Input
    abstract val expectedSha256: Property<String>

    @get:Classpath
    abstract val currentJars: ConfigurableFileCollection

    @get:Input
    abstract val includes: ListProperty<String>

    /** japicmp filters for elements the baseline declares but no extension can link against; each needs a reason. */
    @get:Input
    abstract val excludes: ListProperty<String>

    /**
     * Names of japicmp change types to treat as binary compatible for this surface, because the runtime that loads
     * these extensions doesn't link the way plain JVM class files do.
     */
    @get:Input
    abstract val compatibleChangeTypes: SetProperty<String>

    @get:Classpath
    abstract val japicmpClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val decoded = Base64.getMimeDecoder().decode(encodedBaseline.get().asFile.readBytes())
        val actualSha256 = MessageDigest.getInstance("SHA-256").digest(decoded).joinToString("") { "%02x".format(it) }
        check(actualSha256 == expectedSha256.get()) {
            "ABI baseline ${encodedBaseline.get().asFile.name} has SHA-256 $actualSha256, expected ${expectedSha256.get()}"
        }
        val baseline = temporaryDir.resolve("baseline.jar").apply { writeBytes(decoded) }

        workerExecutor.classLoaderIsolation { classpath.from(japicmpClasspath) }
            .submit(ExtensionAbiComparison::class.java) {
                baselineJar.set(baseline)
                currentJars.from(this@ExtensionAbiCheckTask.currentJars)
                includes.set(this@ExtensionAbiCheckTask.includes)
                excludes.set(this@ExtensionAbiCheckTask.excludes)
                compatibleChangeTypes.set(this@ExtensionAbiCheckTask.compatibleChangeTypes)
                report.set(this@ExtensionAbiCheckTask.report)
                baselineName.set(encodedBaseline.get().asFile.name.removeSuffix(".jar.b64"))
            }
    }
}
