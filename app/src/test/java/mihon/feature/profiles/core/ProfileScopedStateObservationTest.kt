package mihon.feature.profiles.core

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScopedStateObservationTest {

    @Test
    fun `profile switch resets state and ignores values from previous profile`() = runTest {
        val activeProfileId = MutableStateFlow(1L)
        val profileValues = mapOf(
            1L to MutableSharedFlow<String>(extraBufferCapacity = 1),
            2L to MutableSharedFlow(extraBufferCapacity = 1),
        )
        val events = mutableListOf<ProfileScopedStateEvent<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeProfileScopedState(activeProfileId, profileValues::getValue)
                .collect(events::add)
        }

        profileValues.getValue(1L).emit("first")
        activeProfileId.value = 2L
        profileValues.getValue(1L).emit("stale")
        profileValues.getValue(2L).emit("second")

        events shouldBe listOf(
            ProfileScopedStateEvent.Reset(1L),
            ProfileScopedStateEvent.Value(1L, "first"),
            ProfileScopedStateEvent.Reset(2L),
            ProfileScopedStateEvent.Value(2L, "second"),
        )
    }
}
