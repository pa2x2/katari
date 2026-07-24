package mihon.entry.interactions

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.host.EntryMigrationPendingConsequence
import mihon.feature.graph.FeatureDurableExecutionEnvelope
import mihon.feature.graph.FeatureExecutionParticipantId
import org.junit.jupiter.api.Test

class EntryMigrationConsequenceDeliveryTest {
    @Test
    fun `participant failure leaves its opaque consequence durable`() = runTest {
        val host = mockk<EntryMigrationConsequenceHost>()
        val consequences = mockk<EntryMigrationDurableConsequences>()
        val consequence = consequence()
        coEvery { host.pendingConsequences("operation", any()) } returns listOf(consequence)
        coEvery { host.pendingConsequenceCount("operation") } returns 1
        coEvery { host.recordConsequenceFailure(any(), any(), any()) } returns Unit
        coEvery { consequences.deliver(any()) } throws IllegalStateException("not verified")

        delivery(host, consequences).deliverOperation("operation") shouldBe EntryMigrationFollowUp.INCOMPLETE

        coVerify(exactly = 0) { host.acknowledgeConsequence(any()) }
        coVerify(exactly = 1) { host.recordConsequenceFailure(consequence.id, any(), any()) }
    }

    @Test
    fun `successful participant delivery is acknowledged and discarded generically`() = runTest {
        val host = mockk<EntryMigrationConsequenceHost>()
        val consequences = mockk<EntryMigrationDurableConsequences>()
        val consequence = consequence()
        val envelope = consequence.envelope()
        coEvery { host.pendingConsequences("operation", any()) } returns listOf(consequence)
        coEvery { host.pendingConsequenceCount("operation") } returns 0
        coEvery { host.acknowledgeConsequence(consequence.id) } returns Unit
        coEvery { consequences.deliver(envelope) } returns Unit
        coEvery { consequences.discard(listOf(envelope)) } returns Unit

        delivery(host, consequences).deliverOperation("operation") shouldBe EntryMigrationFollowUp.COMPLETE

        coVerify(exactly = 1) { host.acknowledgeConsequence(consequence.id) }
        coVerify(exactly = 1) { consequences.discard(listOf(envelope)) }
        coVerify(exactly = 0) { host.recordConsequenceFailure(any(), any(), any()) }
    }

    private fun delivery(
        host: EntryMigrationConsequenceHost,
        consequences: EntryMigrationDurableConsequences,
    ): EntryMigrationConsequenceDelivery {
        return EntryMigrationConsequenceDelivery(
            host = host,
            consequences = consequences,
            coverOrphanCleanup = mockk(relaxed = true),
            clockMillis = { 1_000 },
        )
    }

    private fun consequence() = EntryMigrationPendingConsequence(
        id = "operation:unknown-participant",
        operationId = "operation",
        profileId = 3,
        participantId = "unknown-participant",
        schemaVersion = 7,
        payload = "opaque",
        attempts = 0,
    )
}

private fun EntryMigrationPendingConsequence.envelope() = FeatureDurableExecutionEnvelope(
    participant = FeatureExecutionParticipantId(participantId),
    schemaVersion = schemaVersion,
    payload = payload,
)
