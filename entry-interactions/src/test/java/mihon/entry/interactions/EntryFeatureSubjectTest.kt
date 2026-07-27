package mihon.entry.interactions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.FeatureIntegrationSubject
import mihon.feature.graph.FeatureSubjectId
import mihon.feature.graph.FeatureSubjectReference
import org.junit.jupiter.api.Test

class EntryFeatureSubjectTest {

    @Test
    fun `Entry boundary rejects an application subject`() {
        val subject = FeatureIntegrationSubject(
            affectedSubject = FeatureSubjectReference(
                id = FeatureSubjectId.Application,
                owner = ContributionOwner("example.application"),
            ),
            feature = FeatureId("example.feature"),
            featureOwner = ContributionOwner("example.feature"),
            integration = FeatureIntegrationId("example.integration"),
        )

        shouldThrow<IllegalStateException> {
            subject.entryContentType
        }.message shouldContain "Expected an Entry content-type Feature subject, received application"
    }
}
