package mihon.entry.interactions.merge.consequence

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.EntryMergeFollowUp
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.merge.host.EntryMergePendingConsequence
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

    private fun consequence() = EntryMergePendingConsequence(
        id = "operation:7:unknown-participant",
        operationId = "operation",
        profileId = 3,
        entryId = 7,
        participantId = "unknown-participant",
        schemaVersion = 5,
        payload = "opaque",
        attempts = 0,
    )
}
