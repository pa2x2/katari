package mihon.entry.interactions

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

internal fun recordingDownloadInteraction(): EntryDownloadInteraction = mockk<EntryDownloadInteraction>(
    relaxed = true,
) {
    every { changes } returns emptyFlow()
    every { queueState } returns flowOf(emptyList())
    every { isInitializing } returns flowOf(false)
    every { isRunning } returns flowOf(false)
    every { isPaused } returns flowOf(false)
}
