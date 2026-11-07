package tachiyomi.core.common.util.lang

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ObservationRecoveryTest {

    @Test
    fun `an observation that fails once goes on with what changes afterwards`() {
        runBlocking {
            var subscriptions = 0
            val observed = flow {
                subscriptions++
                if (subscriptions == 1) {
                    emit("before")
                    error("the query failed")
                }
                emit("after")
                emit("later")
            }

            observed.resubscribeOnFailure(this@ObservationRecoveryTest) { emit("meanwhile") }
                .take(4)
                .toList() shouldBe listOf("before", "meanwhile", "after", "later")
        }
    }
}
