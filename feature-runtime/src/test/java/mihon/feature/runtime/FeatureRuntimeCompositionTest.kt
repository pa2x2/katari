package mihon.feature.runtime

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.feature.graph.ApplicationSubjectContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContentTypeContribution
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.FeatureSubjectScope
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test

class FeatureRuntimeCompositionTest {

    @Test
    fun `one composition evaluates Entry and application subjects from independent installers`() {
        val entryOwner = ContributionOwner("example.book")
        val applicationOwner = ContributionOwner("example.application")
        val featureOwner = ContributionOwner("example.translation")
        val entryInput = FeatureRuntimeInputs(
            graphContributors = listOf(
                featureGraphContributor(entryOwner) {
                    add(
                        ContentTypeContribution(
                            contentType = ContentTypeId("book"),
                            owner = entryOwner,
                        ),
                    )
                },
            ),
        )
        val applicationInput = FeatureRuntimeInputs(
            graphContributors = listOf(
                featureGraphContributor(applicationOwner) {
                    add(ApplicationSubjectContribution(applicationOwner))
                },
                featureGraphContributor(featureOwner) {
                    add(
                        FeatureContribution(
                            feature = FeatureId("translation"),
                            owner = featureOwner,
                            integrations = listOf(
                                FeatureIntegration(
                                    id = FeatureIntegrationId("translation.application"),
                                    prerequisites = CapabilityExpression.Always,
                                    subjectScope = FeatureSubjectScope.Application,
                                    behaviorProjections = listOf(behavior("translation.application")),
                                ),
                                FeatureIntegration(
                                    id = FeatureIntegrationId("translation.entry"),
                                    prerequisites = CapabilityExpression.Always,
                                    subjectScope = FeatureSubjectScope.EntryContentType,
                                    behaviorProjections = listOf(behavior("translation.entry")),
                                ),
                            ),
                        ),
                    )
                },
            ),
        )

        val composition = createFeatureRuntimeComposition(listOf(entryInput, applicationInput))

        composition.graph.subjects.map { it.subject } shouldContainExactly listOf(
            FeatureSubjectId.Application,
            FeatureSubjectId.EntryContentType(ContentTypeId("book")),
        )
        composition.evaluation.integrations.map { it.subject.affectedSubject.id } shouldContainExactly listOf(
            FeatureSubjectId.Application,
            FeatureSubjectId.EntryContentType(ContentTypeId("book")),
        )
        composition.artifacts.obligations shouldBe emptyList()
    }

    private fun behavior(id: String) = object : FeatureBehaviorProjection {
        override val id = FeatureArtifactId(id)
    }
}
