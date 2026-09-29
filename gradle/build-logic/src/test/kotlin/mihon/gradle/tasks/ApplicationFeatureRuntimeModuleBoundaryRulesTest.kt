package mihon.gradle.tasks

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ApplicationFeatureRuntimeModuleBoundaryRulesTest {

    @Test
    fun `runtime modules and descriptors must pair up`() {
        val undescribed = checkApplicationFeatureRuntimeModuleBoundaries(
            validTopology().filterNot { it.relativePath.endsWith(".application-feature-module") },
        )

        undescribed.shouldHaveSize(1)
        undescribed.single().reason shouldContain "missing its owner-local descriptor"

        val sources = validTopology().map { source ->
            if (source.relativePath.endsWith(".application-feature-module")) {
                source.copy(content = "id=example.feature\nmodule=example.MissingRuntimeModule")
            } else {
                source
            }
        }

        val invented = checkApplicationFeatureRuntimeModuleBoundaries(sources)

        invented.shouldHaveSize(2)
        invented.joinToString { it.reason } shouldContain "names no production runtime module"
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
                include("**/*.application-feature-runtime-component")
            """.trimIndent(),
        ),
    )
}
