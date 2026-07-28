package mihon.gradle.tasks

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ApplicationFeatureRuntimeModuleBoundaryRulesTest {

    @Test
    fun `owner-local descriptor registers its production runtime module`() {
        val findings = checkApplicationFeatureRuntimeModuleBoundaries(
            validTopology(),
        )

        findings.shouldBeEmpty()
    }

    @Test
    fun `runtime module without a descriptor fails`() {
        val findings = checkApplicationFeatureRuntimeModuleBoundaries(
            validTopology().filterNot { it.relativePath.endsWith(".application-feature-module") },
        )

        findings.shouldHaveSize(1)
        findings.single().reason shouldContain "missing its owner-local descriptor"
    }

    @Test
    fun `descriptor must resolve a module from the same owner`() {
        val sources = validTopology().map { source ->
            if (source.relativePath.endsWith(".application-feature-module")) {
                source.copy(relativePath = "other/src/main/resources/example.application-feature-module")
            } else {
                source
            }
        }

        val findings = checkApplicationFeatureRuntimeModuleBoundaries(sources)

        findings.shouldHaveSize(1)
        findings.single().reason shouldContain "must live in feature-example"
    }

    @Test
    fun `descriptor cannot invent a production module`() {
        val sources = validTopology().map { source ->
            if (source.relativePath.endsWith(".application-feature-module")) {
                source.copy(content = "id=example.feature\nmodule=example.MissingRuntimeModule")
            } else {
                source
            }
        }

        val findings = checkApplicationFeatureRuntimeModuleBoundaries(sources)

        findings.shouldHaveSize(2)
        findings.joinToString { it.reason } shouldContain "names no production runtime module"
    }

    @Test
    fun `runtime infrastructure requires generated app topology`() {
        val findings = checkApplicationFeatureRuntimeModuleBoundaries(
            listOf(
                ApplicationFeatureRuntimeModuleBoundarySource(
                    relativePath = "feature-runtime/src/main/java/example/ApplicationFeatureRuntimeModule.kt",
                    content = "package example\nclass ApplicationFeatureRuntimeModule()",
                ),
                ApplicationFeatureRuntimeModuleBoundarySource(
                    relativePath = "app/build.gradle.kts",
                    content = "plugins {}",
                ),
            ),
        )

        findings.shouldHaveSize(1)
        findings.single().reason shouldContain "must generate its Application Feature topology"
    }

    private fun validTopology(): List<ApplicationFeatureRuntimeModuleBoundarySource> = listOf(
        ApplicationFeatureRuntimeModuleBoundarySource(
            relativePath = "feature-example/src/main/java/example/ExampleRuntimeModule.kt",
            content = """
                package example

                internal val ExampleRuntimeModule = ApplicationFeatureRuntimeModule(
                    id = "example.feature",
                    contributor = ExampleContributor,
                ) { ApplicationFeatureRuntimeArtifacts() }
            """.trimIndent(),
        ),
        ApplicationFeatureRuntimeModuleBoundarySource(
            relativePath = "feature-example/src/main/resources/example.application-feature-module",
            content = "id=example.feature\nmodule=example.ExampleRuntimeModule",
        ),
        ApplicationFeatureRuntimeModuleBoundarySource(
            relativePath = "app/build.gradle.kts",
            content = """
                import mihon.gradle.tasks.GenerateApplicationFeatureTopologyTask
                include("**/*.application-feature-module")
            """.trimIndent(),
        ),
    )
}
