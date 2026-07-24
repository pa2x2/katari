package eu.kanade.tachiyomi.documentation

import mihon.entry.interactions.documentation.rendering.renderEntryContentTypeReferenceMarkdown
import mihon.entry.interactions.documentation.rendering.replaceEntryContentTypeReferenceGeneratedRegion
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProductionEntryContentTypeReferenceDocumentationTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `generate or verify production content type reference`() {
        val mode = System.getProperty(MODE_PROPERTY)
        assumeTrue(mode != null, "Executed only by the content-type-reference documentation tasks")
        val referenceFile = File(requireNotNull(System.getProperty(FILE_PROPERTY)))

        ProductionEntryDocumentationEnvironment(temporaryDirectory).use { environment ->
            val generated = renderEntryContentTypeReferenceMarkdown(environment.contentTypeReferencePlan())
            val current = referenceFile.readText()
            val expected = replaceEntryContentTypeReferenceGeneratedRegion(current, generated)
            when (mode) {
                GENERATE_MODE -> referenceFile.writeText(expected)
                VERIFY_MODE -> check(current == expected) {
                    "Content type reference is stale. Run ./gradlew generateContentTypeReference."
                }
                else -> error("Unknown content-type-reference documentation mode: $mode")
            }
        }
    }

    private companion object {
        const val MODE_PROPERTY = "mihon.entry.contentTypeReference.mode"
        const val FILE_PROPERTY = "mihon.entry.contentTypeReference.file"
        const val GENERATE_MODE = "generate"
        const val VERIFY_MODE = "verify"
    }
}
