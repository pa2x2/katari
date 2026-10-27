package mihon.gradle.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VendoredScriptsCheckTaskTest {

    @Test
    fun `a bundle edited after its build is reported, paths resolved from the manifest's directory`(
        @TempDir root: File,
    ) {
        val bundle = root.resolve("module/script-bundle").apply { mkdirs() }
        val output = root.resolve("module/src/main/resources/lib.js").apply {
            parentFile.mkdirs()
            writeText("built")
        }
        val manifest = bundle.resolve("SHA256SUMS").apply {
            // sha256("built")
            writeText(
                "586a866f990ab55e36decfffc2011f172e4452d5141c939a5436baffba11111d  ../src/main/resources/lib.js\n",
            )
        }
        val built = VendoredScriptsCheckTask.problemsIn(manifest)

        output.writeText("edited")
        val edited = VendoredScriptsCheckTask.problemsIn(manifest)

        assertEquals(emptyList<String>(), built)
        assertEquals(listOf("${output.path}: SHA-256 differs from ${manifest.path}"), edited)
    }
}
