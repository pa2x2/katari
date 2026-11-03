package mihon.model.artifacts.runtime.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.InstalledModelArtifact
import mihon.model.artifacts.api.state.ModelArtifactFailure
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.model.artifacts.runtime.download.ModelArtifactFileDownloadResult
import mihon.model.artifacts.runtime.download.ModelArtifactFileDownloader
import mihon.model.artifacts.runtime.network.ModelArtifactNetworkPolicy
import mihon.model.artifacts.runtime.storage.ModelArtifactStorage
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal class DefaultModelArtifactStore(
    private val storage: ModelArtifactStorage,
    private val downloader: ModelArtifactFileDownloader,
    private val networkPolicy: ModelArtifactNetworkPolicy,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : ModelArtifactStore {
    /** Download activity that disk state cannot express: progress and the last failure. */
    private val activity = MutableStateFlow<Map<RevisionKey, ModelArtifactState>>(emptyMap())
    private val storageVersion = MutableStateFlow(0L)
    private val jobs = ConcurrentHashMap<RevisionKey, Job>()

    override fun observe(descriptor: ModelArtifactDescriptor): Flow<ModelArtifactState> {
        val key = RevisionKey(descriptor)
        return combine(activity.map { it[key] }.distinctUntilChanged(), storageVersion) { transient, _ ->
            transient ?: storedState(descriptor)
        }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun installed(descriptor: ModelArtifactDescriptor): InstalledModelArtifact? =
        withContext(ioDispatcher) { storage.installed(descriptor) }

    override fun download(approval: ModelArtifactDownloadApproval) {
        val key = RevisionKey(approval.artifact)
        jobs.compute(key) { _, running ->
            running?.takeIf(Job::isActive) ?: scope.launch(ioDispatcher) {
                try {
                    install(key, approval)
                } finally {
                    jobs.remove(key, coroutineContext[Job])
                }
            }
        }
    }

    override fun cancel(artifact: ModelArtifactDescriptor) {
        jobs[RevisionKey(artifact)]?.cancel()
    }

    override suspend fun delete(artifact: ModelArtifactId) {
        jobs.filterKeys { it.id == artifact }.values.forEach { it.cancelAndJoin() }
        withContext(ioDispatcher) { storage.delete(artifact) }
        activity.update { states -> states.filterKeys { it.id != artifact } }
        storageVersion.update { it + 1 }
    }

    override fun observeStored(): Flow<List<StoredModelArtifact>> =
        storageVersion.map { storage.stored() }.flowOn(ioDispatcher)

    private suspend fun install(key: RevisionKey, approval: ModelArtifactDownloadApproval) {
        val descriptor = approval.artifact
        if (storage.installed(descriptor) != null) {
            settle(key)
            return
        }
        if (!approval.allowMeteredNetwork && networkPolicy.isActiveNetworkMetered()) {
            fail(key, ModelArtifactFailure.MeteredNetwork)
            return
        }
        publish(key, ModelArtifactState.Downloading(0, descriptor.sizeBytes))
        try {
            storage.begin(descriptor)
            val requiredBytes = descriptor.sizeBytes - storage.storedBytes(descriptor)
            val availableBytes = storage.usableBytes()
            if (requiredBytes > availableBytes) {
                fail(key, ModelArtifactFailure.InsufficientStorage(requiredBytes, availableBytes))
                return
            }

            var completedBytes = 0L
            descriptor.files.forEach { file ->
                val result = downloader.download(file, storage.target(descriptor, file)) { downloaded ->
                    publish(key, ModelArtifactState.Downloading(completedBytes + downloaded, descriptor.sizeBytes))
                }
                when (result) {
                    ModelArtifactFileDownloadResult.Completed -> completedBytes += file.sizeBytes
                    ModelArtifactFileDownloadResult.ChecksumMismatch ->
                        return fail(key, ModelArtifactFailure.ChecksumMismatch(file.name))
                    is ModelArtifactFileDownloadResult.NetworkFailed ->
                        return fail(key, ModelArtifactFailure.Network(result.message))
                    is ModelArtifactFileDownloadResult.StorageFailed ->
                        return fail(key, ModelArtifactFailure.Storage(result.message))
                }
            }
            storage.commit(descriptor)
            settle(key)
        } catch (error: CancellationException) {
            settle(key)
            throw error
        } catch (error: IOException) {
            fail(key, ModelArtifactFailure.Storage(error.message))
        } catch (error: IllegalStateException) {
            fail(key, ModelArtifactFailure.Storage(error.message))
        }
    }

    private fun publish(key: RevisionKey, state: ModelArtifactState) {
        activity.update { it + (key to state) }
    }

    private fun fail(key: RevisionKey, failure: ModelArtifactFailure) {
        publish(key, ModelArtifactState.Failed(failure))
        storageVersion.update { it + 1 }
    }

    /** Clears transient activity so observers fall back to what storage now holds. */
    private fun settle(key: RevisionKey) {
        activity.update { it - key }
        storageVersion.update { it + 1 }
    }

    private fun storedState(descriptor: ModelArtifactDescriptor): ModelArtifactState =
        storage.installed(descriptor)?.let(ModelArtifactState::Installed) ?: ModelArtifactState.NotInstalled

    private data class RevisionKey(
        val id: ModelArtifactId,
        val revision: String,
    ) {
        constructor(descriptor: ModelArtifactDescriptor) : this(descriptor.id, descriptor.revision)
    }
}
