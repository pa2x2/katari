package mihon.entry.interactions

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergePendingConsequence
import mihon.feature.graph.FeatureDurableExecutionEnvelope
import mihon.feature.graph.FeatureExecutionParticipantId
import org.junit.jupiter.api.Test

class EntryMergeConsequenceDeliveryTest {
    @Test
    fun `participant failure leaves its opaque consequence durable`() = runTest {
        val host = mockk<EntryMergeHost>()
        val consequences = mockk<EntryMergeDurableConsequences>()
        val consequence = consequence()
        coEvery { host.pendingConsequences(any()) } returns listOf(consequence)
        coEvery { host.pendingConsequenceCount("operation") } returns 1
        coEvery { host.recordConsequenceFailure(any(), any(), any()) } returns Unit
        coEvery { consequences.deliver(any()) } throws IllegalStateException("unknown participant")

        EntryMergeConsequenceDelivery(host, consequences).deliverOperation("operation") shouldBe
            EntryMergeFollowUp.PENDING

        coVerify(exactly = 0) { host.acknowledgeConsequence(any()) }
        coVerify(exactly = 1) { host.recordConsequenceFailure(consequence.id, any(), any()) }
    }

    @Test
    fun `successful participant delivery is acknowledged and discarded generically`() = runTest {
        val host = mockk<EntryMergeHost>()
        val consequences = mockk<EntryMergeDurableConsequences>()
        val consequence = consequence()
        val envelope = consequence.envelope()
        coEvery { host.pendingConsequences(any()) } returns listOf(consequence)
        coEvery { host.pendingConsequenceCount("operation") } returns 0
        coEvery { host.acknowledgeConsequence(consequence.id) } returns Unit
        coEvery { consequences.deliver(envelope) } returns Unit
        coEvery { consequences.discardEnvelope(envelope) } returns Unit

        EntryMergeConsequenceDelivery(host, consequences).deliverOperation("operation") shouldBe
            EntryMergeFollowUp.COMPLETE

        coVerify(exactly = 1) { host.acknowledgeConsequence(consequence.id) }
        coVerify(exactly = 1) { consequences.discardEnvelope(envelope) }
        coVerify(exactly = 0) { host.recordConsequenceFailure(any(), any(), any()) }
    }

    @Test
    fun `schema one compatibility is isolated from current participant delivery`() = runTest {
        val host = mockk<EntryMergeHost>()
        val consequences = mockk<EntryMergeDurableConsequences>()
        val legacy = mockk<EntryMergeLegacyConsequenceDelivery>()
        val consequence = consequence(
            participantId = ENTRY_TRACKING_MERGE_PARTICIPANT.id.value,
            schemaVersion = 1,
        )
        coEvery { host.pendingConsequences(any()) } returns listOf(consequence)
        coEvery { host.pendingConsequenceCount("operation") } returns 0
        coEvery { host.acknowledgeConsequence(consequence.id) } returns Unit
        every { legacy.handles(consequence) } returns true
        coEvery { legacy.deliver(consequence) } returns Unit

        EntryMergeConsequenceDelivery(host, consequences, legacy).deliverOperation("operation") shouldBe
            EntryMergeFollowUp.COMPLETE

        coVerify(exactly = 1) { legacy.deliver(consequence) }
        coVerify(exactly = 0) { consequences.deliver(any()) }
        coVerify(exactly = 0) { consequences.discardEnvelope(any()) }
    }

    private fun consequence(
        participantId: String = "unknown-participant",
        schemaVersion: Int = 5,
    ) = EntryMergePendingConsequence(
        id = "operation:7:$participantId",
        operationId = "operation",
        profileId = 3,
        entryId = 7,
        participantId = participantId,
        schemaVersion = schemaVersion,
        payload = "opaque",
        attempts = 0,
    )
}

private fun EntryMergePendingConsequence.envelope() = FeatureDurableExecutionEnvelope(
    participant = FeatureExecutionParticipantId(participantId),
    schemaVersion = schemaVersion,
    payload = payload,
)
