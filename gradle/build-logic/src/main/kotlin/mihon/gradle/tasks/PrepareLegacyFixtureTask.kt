package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest
import java.util.Base64

@CacheableTask
abstract class PrepareLegacyFixtureTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val encodedFixture: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val expectedSha256: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun prepare() {
        val decoded = Base64.getMimeDecoder().decode(encodedFixture.get().asFile.readBytes())
        expectedSha256.orNull?.let { expected ->
            val actual = MessageDigest.getInstance("SHA-256")
                .digest(decoded)
                .joinToString("") { "%02x".format(it) }
            check(actual == expected) {
                "Legacy fixture has SHA-256 $actual, expected $expected"
            }
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeBytes(decoded)
        }
    }
}
