package mihon.entry.interactions.manga.page.acquisition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.core.common.image.progressive.ProgressiveImageDecodeOptions
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImageSession
import mihon.core.common.image.progressive.ProgressiveImageState
import mihon.entry.interactions.manga.page.MangaPageStore
import okhttp3.Response
import java.io.Closeable
import java.io.File

internal class MangaPageAcquisitionCoordinator(
    private val store: MangaPageStore,
    private val progressiveImageEngine: ProgressiveImageEngine,
) : MangaPageAcquirer, Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, InFlightAcquisition>()

    override suspend fun acquire(
        imageUrl: String,
        force: Boolean,
        options: ProgressiveImageDecodeOptions,
        onFetch: () -> Unit,
        onProgressiveState: (StateFlow<ProgressiveImageState>?) -> Unit,
        fetch: suspend () -> Response,
    ): File {
        while (true) {
            if (!force) {
                store.getCommittedImage(imageUrl)?.let {
                    onProgressiveState(null)
                    return it
                }
            }

            when (val selection = selectAcquisition(imageUrl, force, options, fetch)) {
                is AcquisitionSelection.AwaitTeardown -> {
                    selection.result.join()
                    currentCoroutineContext().ensureActive()
                }
                is AcquisitionSelection.Use -> {
                    onFetch()
                    return awaitAcquisition(selection.acquisition, onProgressiveState)
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    private suspend fun selectAcquisition(
        imageUrl: String,
        force: Boolean,
        options: ProgressiveImageDecodeOptions,
        fetch: suspend () -> Response,
    ): AcquisitionSelection = inFlightMutex.withLock {
        val existing = inFlight[imageUrl]
        when {
            existing == null -> AcquisitionSelection.Use(createAcquisition(imageUrl, force, options, fetch))
            existing.result.isActive -> {
                existing.subscribers++
                AcquisitionSelection.Use(existing)
            }
            !existing.result.isCompleted -> AcquisitionSelection.AwaitTeardown(existing.result)
            else -> {
                inFlight.remove(imageUrl, existing)
                AcquisitionSelection.Use(createAcquisition(imageUrl, force, options, fetch))
            }
        }
    }

    private suspend fun awaitAcquisition(
        acquisition: InFlightAcquisition,
        onProgressiveState: (StateFlow<ProgressiveImageState>?) -> Unit,
    ): File {
        return try {
            onProgressiveState(acquisition.progressiveState)
            acquisition.result.await()
        } finally {
            withContext(NonCancellable) {
                release(acquisition)
            }
        }
    }

    private fun createAcquisition(
        imageUrl: String,
        force: Boolean,
        options: ProgressiveImageDecodeOptions,
        fetch: suspend () -> Response,
    ): InFlightAcquisition {
        val progressiveSession = progressiveImageEngine.openSession(options)
        val result = scope.async(start = CoroutineStart.LAZY) {
            download(imageUrl, force, fetch, progressiveSession)
        }
        val acquisition = InFlightAcquisition(
            result = result,
            progressiveState = progressiveSession?.state,
            subscribers = 1,
        )
        inFlight[imageUrl] = acquisition
        result.invokeOnCompletion {
            scope.launch {
                inFlightMutex.withLock {
                    inFlight.remove(imageUrl, acquisition)
                }
            }
        }
        result.start()
        return acquisition
    }

    private suspend fun download(
        imageUrl: String,
        force: Boolean,
        fetch: suspend () -> Response,
        progressiveSession: ProgressiveImageSession?,
    ): File {
        try {
            if (!force) store.getCommittedImage(imageUrl)?.let { return it }
            val response = fetch()
            val coroutineContext = currentCoroutineContext()
            return runInterruptible {
                response.use {
                    store.beginImageWrite(imageUrl).use { stagingWrite ->
                        val buffer = ByteArray(DOWNLOAD_COPY_BUFFER_SIZE)
                        response.body.source().use { input ->
                            while (true) {
                                coroutineContext.ensureActive()
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                stagingWrite.write(buffer, bytesRead)
                                progressiveSession?.append(buffer, length = bytesRead)
                            }
                        }
                        coroutineContext.ensureActive()
                        progressiveSession?.finish()
                        stagingWrite.commit()
                    }
                }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                progressiveSession?.close()
            } else {
                progressiveSession?.fail(error)
            }
            throw error
        } finally {
            progressiveSession?.close()
        }
    }

    private suspend fun release(acquisition: InFlightAcquisition) {
        inFlightMutex.withLock {
            acquisition.subscribers--
            if (acquisition.subscribers == 0 && acquisition.result.isActive) {
                acquisition.result.cancel()
            }
        }
    }

    private sealed interface AcquisitionSelection {
        data class Use(
            val acquisition: InFlightAcquisition,
        ) : AcquisitionSelection

        data class AwaitTeardown(
            val result: Deferred<File>,
        ) : AcquisitionSelection
    }

    private class InFlightAcquisition(
        val result: Deferred<File>,
        val progressiveState: StateFlow<ProgressiveImageState>?,
        var subscribers: Int,
    )
}

private const val DOWNLOAD_COPY_BUFFER_SIZE = 8_192
