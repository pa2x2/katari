package tachiyomi.core.common.preference

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PreferenceBoundsTest {

    @Test
    fun `bounded preference clamps default value and reads`() {
        val preference = TestIntPreference(initialDefault = 75).coerceIn(1..50)

        preference.defaultValue() shouldBe 50
        preference.get() shouldBe 50
    }

    @Test
    fun `bounded preference clamps writes and change emissions`() = runBlocking {
        val delegate = TestIntPreference(initialDefault = 5)
        val preference = delegate.coerceIn(1..50)
        val values = mutableListOf<Int>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            preference.changes().take(3).toList(values)
        }

        preference.set(75)
        delegate.get() shouldBe 50

        delegate.set(-3)

        job.join()
        values shouldBe listOf(5, 50, 1)
    }

    private class TestIntPreference(
        private val initialDefault: Int,
        initialValue: Int? = null,
    ) : Preference<Int> {
        private val state = MutableStateFlow(initialValue)

        override fun key(): String = "test"

        override fun get(): Int = state.value ?: initialDefault

        override fun set(value: Int) {
            state.value = value
        }

        override fun isSet(): Boolean = state.value != null

        override fun delete() {
            state.value = null
        }

        override fun defaultValue(): Int = initialDefault

        override fun changes(): Flow<Int> = state.asStateFlow().map { it ?: initialDefault }

        override fun stateIn(scope: CoroutineScope): StateFlow<Int> {
            return changes().stateIn(scope, SharingStarted.Eagerly, get())
        }
    }
}
