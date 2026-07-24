package mihon.feature.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FeatureExecutionLifecycleTest {

    private val pointOwner = ContributionOwner("test.lifecycle")
    private val participantOwner = ContributionOwner("test.participant")
    private val contentType = ContentTypeId("test.type")
    private val transactionalPoint = transactionalFeatureExecutionPointDefinition<Event>(
        id = FeatureExecutionPointId("test.lifecycle.transactional"),
        owner = pointOwner,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )
    private val afterCommitPoint = afterCommitVolatileFeatureExecutionPointDefinition<Event>(
        id = FeatureExecutionPointId("test.lifecycle.after-commit"),
        owner = pointOwner,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )
    private val transactionalParticipant = participant("test.participant.transactional", transactionalPoint)
    private val afterCommitParticipant = participant("test.participant.after-commit", afterCommitPoint)

    @Test
    fun `successful host commit releases volatile consequences after transactional work`() = runTest {
        val trace = mutableListOf<String>()
        val runtime = runtime(trace)

        val result = runtime.coordinateFeatureCommit(
            commit = {
                val insideTransaction = callback {
                    runtimeResult(
                        execute(transactionalPoint, contentType, Event("transactional")),
                    )
                }
                trace += "host-transaction-started"
                insideTransaction()
                trace += "host-transaction-committed"
                Commit.Applied
            },
            committed = { it == Commit.Applied },
            volatileConsequences = {
                runtimeResult(execute(afterCommitPoint, contentType, Event("after-commit")))
            },
        )

        (result is FeatureCommitExecutionResult.Committed) shouldBe true
        trace shouldContainExactly listOf(
            "host-transaction-started",
            "transactional",
            "host-transaction-committed",
            "after-commit",
        )
    }

    @Test
    fun `rolled back host transaction suppresses volatile consequences`() = runTest {
        val trace = mutableListOf<String>()
        val runtime = runtime(trace)

        val result = runtime.coordinateFeatureCommit(
            commit = {
                val insideTransaction = callback {
                    runtimeResult(
                        execute(transactionalPoint, contentType, Event("transactional")),
                    )
                }
                insideTransaction()
                Commit.RolledBack
            },
            committed = { it == Commit.Applied },
            volatileConsequences = {
                runtimeResult(execute(afterCommitPoint, contentType, Event("after-commit")))
            },
        )

        result shouldBe FeatureCommitExecutionResult.NotCommitted(Commit.RolledBack)
        trace shouldContainExactly listOf("transactional")
    }

    @Test
    fun `host transaction callback cannot escape or execute twice`() = runTest {
        val runtime = runtime(mutableListOf())
        lateinit var transactionCallback: suspend () -> Unit

        runtime.coordinateFeatureCommit(
            commit = {
                transactionCallback = callback {
                    runtimeResult(
                        execute(transactionalPoint, contentType, Event("transactional")),
                    )
                }
                transactionCallback()
                shouldThrow<IllegalStateException> { transactionCallback() }
                Commit.Applied
            },
            committed = { it == Commit.Applied },
            volatileConsequences = { Unit },
        )

        shouldThrow<IllegalStateException> { transactionCallback() }
    }

    private fun runtime(trace: MutableList<String>): FeatureExecutionRuntime {
        val typeOwner = ContributionOwner("test.type-owner")
        val graph = discoverAndAssembleFeatureGraph(
            listOf(
                featureGraphContributor(typeOwner) {
                    add(ContentTypeContribution(contentType, typeOwner, emptyList()))
                },
                featureGraphContributor(pointOwner) {
                    add(transactionalPoint)
                    add(afterCommitPoint)
                },
                featureGraphContributor(participantOwner) {
                    add(transactionalParticipant)
                    add(afterCommitParticipant)
                },
            ),
        )
        return FeatureExecutionRuntime(
            graph = graph,
            evaluation = evaluateFeatureGraph(graph),
            bindings = listOf(
                FeatureExecutionParticipantBinding(
                    transactionalParticipant,
                    FeatureExecutionHandler { trace += it.value },
                ),
                FeatureExecutionParticipantBinding(
                    afterCommitParticipant,
                    FeatureExecutionHandler { trace += it.value },
                ),
            ),
        )
    }

    private fun <E : Any> participant(
        id: String,
        point: FeatureExecutionPointDefinition<E>,
    ): FeatureExecutionParticipantDefinition<E> = FeatureExecutionParticipantDefinition(
        id = FeatureExecutionParticipantId(id),
        owner = participantOwner,
        point = point,
        behavioralContracts = listOf(LifecycleContract),
    )

    private fun runtimeResult(result: FeatureExecutionResult) {
        check(result.isSuccessful) {
            result.failures.joinToString { "${it.participant}: ${it.error.message}" }
        }
    }

    private data class Event(val value: String)

    private enum class Commit {
        Applied,
        RolledBack,
    }

    private object LifecycleContract : FeatureBehaviorContract {
        override val id = FeatureArtifactId("test.lifecycle.behavior")
    }
}
