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
import mihon.feature.runtime.createFeatureRuntimeComposition
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
    fun `an empty module topology still installs the application subject`() {
        val installation = installApplicationFeatureRuntimeModules(
            registrar = registrar,
            modules = emptyList(),
            context = context,
        )

        val discovered = discoverFeatureGraphContributions(installation.featureRuntimeInputs.graphContributors)

        discovered.applicationSubjects.size shouldBe 1
        discovered.applicationSubjects.single().providers shouldBe emptyList()
    }

    @Test
    fun `capability ownership cannot be ambiguous across modules`() {
        val capability = capabilityDefinition<FirstProvider>(
            CapabilityId("example.shared"),
            ContributionOwner("example.shared-contract"),
        )

        val error = shouldThrow<IllegalArgumentException> {
            installApplicationFeatureRuntimeModules(
                registrar = registrar,
                modules = listOf(
                    module("example.first") {
                        ApplicationFeatureRuntimeArtifacts(
                            capabilityProviders = listOf(
                                CapabilityProvider(
                                    capability,
                                    FirstProvider(),
                                ),
                            ),
                        )
                    },
                    module("example.second") {
                        ApplicationFeatureRuntimeArtifacts(
                            capabilityProviders = listOf(
                                CapabilityProvider(
                                    capability,
                                    FirstProvider(),
                                ),
                            ),
                        )
                    },
                ),
                context = context,
            )
        }

        error.message shouldContain "Capability providers for application must have unique ids"
    }

    @Test
    fun `runtime boundary ownership cannot be ambiguous`() {
        val error = shouldThrow<IllegalStateException> {
            installApplicationFeatureRuntimeModules(
                registrar = registrar,
                modules = listOf(
                    module("example.first") {
                        ApplicationFeatureRuntimeArtifacts(
                            runtimeBoundaries = listOf(
                                applicationFeatureRuntimeBoundary<SharedBoundary> { SharedBoundary() },
                            ),
                        )
                    },
                    module("example.second") {
                        ApplicationFeatureRuntimeArtifacts(
                            runtimeBoundaries = listOf(
                                applicationFeatureRuntimeBoundary<SharedBoundary> { SharedBoundary() },
                            ),
                        )
                    },
                ),
                context = context,
            )
        }

        error.message shouldContain "runtime boundaries are installed by multiple modules"
    }

    @Test
    fun `installed modules validate the assembled application graph`() {
        var validated = false
        val installation = installApplicationFeatureRuntimeModules(
            registrar = registrar,
            modules = listOf(
                module("example.graph") {
                    ApplicationFeatureRuntimeArtifacts(
                        graphValidators = listOf(
                            ApplicationFeatureRuntimeGraphValidator { evaluation ->
                                evaluation.obligations shouldBe emptyList()
                                validated = true
                            },
                        ),
                    )
                },
            ),
            context = context,
        )
        val composition = createFeatureRuntimeComposition(listOf(installation.featureRuntimeInputs))

        validateInstalledApplicationFeatureRuntimeGraph(installation, composition.evaluation)

        validated shouldBe true
    }

    @Test
    fun `runtime components expose only requested typed participation`() {
        val components = ApplicationFeatureRuntimeComponents(
            listOf(
                RegisteredApplicationFeatureRuntimeComponent(
                    "example.first",
                    FirstRuntimeComponent(),
                ),
                RegisteredApplicationFeatureRuntimeComponent(
                    "example.second",
                    SecondRuntimeComponent(),
                ),
            ),
        )

        components.instances<FirstRuntimeComponent>().size shouldBe 1
        components.instances<SecondRuntimeComponent>().size shouldBe 1
        components.instances<MissingRuntimeComponent>() shouldBe emptyList()
    }

    @Test
    fun `runtime component ids must be unique`() {
        val error = shouldThrow<IllegalArgumentException> {
            ApplicationFeatureRuntimeComponents(
                listOf(
                    RegisteredApplicationFeatureRuntimeComponent(
                        "example.same",
                        FirstRuntimeComponent(),
                    ),
                    RegisteredApplicationFeatureRuntimeComponent(
                        "example.same",
                        SecondRuntimeComponent(),
                    ),
                ),
            )
        }

        error.message shouldContain "Duplicate Application Feature runtime components"
    }

    private fun module(
        id: String,
        installRuntime:
        InjektRegistrar.(ApplicationFeatureRuntimeInstallationContext) -> ApplicationFeatureRuntimeArtifacts,
    ): ApplicationFeatureRuntimeModule {
        val owner = ContributionOwner(id)
        return ApplicationFeatureRuntimeModule(
            id = id,
            contributor = emptyContributor(owner),
            installRuntime = installRuntime,
        )
    }

    private fun emptyContributor(owner: ContributionOwner): FeatureGraphContributor =
        featureGraphContributor(owner) {}

    private class FirstProvider

    private class SecondProvider

    private class SharedBoundary

    private class FirstRuntimeComponent : ApplicationFeatureRuntimeComponent

    private class SecondRuntimeComponent : ApplicationFeatureRuntimeComponent

    private class MissingRuntimeComponent : ApplicationFeatureRuntimeComponent
}
