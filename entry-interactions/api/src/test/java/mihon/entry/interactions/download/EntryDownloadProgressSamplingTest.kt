package mihon.entry.interactions.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class EntryDownloadProgressSamplingTest {

    @Test
    fun `a held tick never overwrites a later state change and concurrent downloads keep their latest tick`() =
        runTest {
            val emitted = flow {
                emit(status(1, EntryDownloadState.DOWNLOADING, 0))
                emit(status(2, EntryDownloadState.DOWNLOADING, 0))
                emit(status(1, EntryDownloadState.DOWNLOADING, 10))
                emit(status(2, EntryDownloadState.DOWNLOADING, 90))
                emit(status(1, EntryDownloadState.DOWNLOADING, 20))
                emit(status(2, EntryDownloadState.DOWNLOADED))
                delay(300.milliseconds)
            }
                .sampleProgress(200.milliseconds)
                .toList()

            emitted.shouldContainExactly(
                status(1, EntryDownloadState.DOWNLOADING, 0),
                status(2, EntryDownloadState.DOWNLOADING, 0),
                status(2, EntryDownloadState.DOWNLOADED),
                status(1, EntryDownloadState.DOWNLOADING, 20),
            )
        }

    private fun status(chapterId: Long, state: EntryDownloadState, progress: Int = 0) =
        EntryDownloadStatus(EntryType.MANGA, chapterId, state, progress)
}
