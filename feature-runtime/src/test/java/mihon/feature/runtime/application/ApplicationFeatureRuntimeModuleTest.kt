package mihon.feature.runtime.application

import android.app.Application
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import mihon.feature.graph.CapabilityId
import mihon.feature.graph.CapabilityProvider
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.capabilityDefinition
import mihon.feature.graph.discoverFeatureGraphContributions
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import uy.kohesive.injekt.api.InjektRegistrar

class ApplicationFeatureRuntimeModuleTest {

    private val registrar = mockk<InjektRegistrar>(relaxed = true)
    private val context = ApplicationFeatureRuntimeInstallationContext(
        application = mockk<Application>(relaxed = true),
        dependencies = mockk(relaxed = true),
    )

    @Test
    fun `installed modules aggregate their providers into exactly one application subject`() {
        val firstCapability = capabilityDefinition<FirstProvider>(
            CapabilityId("example.first"),
            ContributionOwner("example.first-contract"),
        )
        val secondCapability = capabilityDefinition<SecondProvider>(
            CapabilityId("example.second"),
            ContributionOwner("example.second-contract"),
        )
        val installation = installApplicationFeatureRuntimeModules(
            registrar = registrar,
            modules = listOf(
                module("example.first") {
                    ApplicationFeatureRuntimeArtifacts(
                        capabilityProviders = listOf(
                            CapabilityProvider(
                                firstCapability,
                                FirstProvider(),
                            ),
                        ),
                    )
                },
                module("example.second") {
                    ApplicationFeatureRuntimeArtifacts(
                        capabilityProviders = listOf(
                            CapabilityProvider(
                                secondCapability,
                                SecondProvider(),
                            ),
                        ),
                    )
                },
            ),
            context = context,
        )

        val discovered = discoverFeatureGraphContributions(installation.featureRuntimeInputs.graphContributors)

        discovered.applicationSubjects.size shouldBe 1
        discovered.applicationSubjects.single().providers.map { it.capability.id } shouldContainExactly listOf(
            firstCapability.id,
            secondCapability.id,
        )
    }

    @Test
    fun `required modules are installed before the modules that require them`() {
        val installedOrder = mutableListOf<String>()

        installApplicationFeatureRuntimeModules(
            registrar = registrar,
            modules = listOf(
                module("example.consumer", requiredModules = setOf("example.provider")) {
                    installedOrder += "example.consumer"
                    ApplicationFeatureRuntimeArtifacts()
                },
                module("example.independent") {
                    installedOrder += "example.independent"
                    ApplicationFeatureRuntimeArtifacts()
                },
                module("example.provider") {
                    installedOrder += "example.provider"
                    ApplicationFeatureRuntimeArtifacts()
                },
            ),
            context = context,
        )

        installedOrder shouldContainExactly listOf("example.provider", "example.consumer", "example.independent")
    }

    @Test
    fun `missing and cyclic module requirements fail installation`() {
        shouldThrow<IllegalStateException> {
            installApplicationFeatureRuntimeModules(
                registrar = registrar,
                modules = listOf(
                    module("example.consumer", requiredModules = setOf("example.missing")) {
                        ApplicationFeatureRuntimeArtifacts()
                    },
                ),
                context = context,
            )
        }.message shouldContain "requires modules that are not installed: [example.missing]"

        shouldThrow<IllegalStateException> {
            installApplicationFeatureRuntimeModules(
                registrar = registrar,
                modules = listOf(
                    module("example.first", requiredModules = setOf("example.second")) {
                        ApplicationFeatureRuntimeArtifacts()
                    },
                    module("example.second", requiredModules = setOf("example.first")) {
                        ApplicationFeatureRuntimeArtifacts()
                    },
                ),
                context = context,
            )
        }.message shouldContain "requirement cycle: example.first -> example.second -> example.first"
    }

    private fun module(
        id: String,
        requiredModules: Set<String> = emptySet(),
        installRuntime:
        InjektRegistrar.(ApplicationFeatureRuntimeInstallationContext) -> ApplicationFeatureRuntimeArtifacts,
    ): ApplicationFeatureRuntimeModule {
        val owner = ContributionOwner(id)
        return ApplicationFeatureRuntimeModule(
            id = id,
            contributor = emptyContributor(owner),
            requiredModules = requiredModules,
            installRuntime = installRuntime,
        )
    }

    private fun emptyContributor(owner: ContributionOwner): FeatureGraphContributor =
        featureGraphContributor(owner) {}

    private class FirstProvider

    private class SecondProvider
}
