package eu.kanade.tachiyomi.extension.installer

import android.app.Service
import android.net.Uri
import androidx.annotation.CallSuper
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import java.util.Collections
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Base implementation class for extension installer. To be used inside a foreground [Service].
 */
@OptIn(ExperimentalAtomicApi::class)
internal abstract class Installer(private val service: Service) {

    private val extensionManager: ExtensionManager by lazy { service.appGraph.extensionManager }

    private var waitingInstall = AtomicReference<Entry?>(null)

    // Guarded by itself: a failed install continues the queue from the install scope, not only main
    private val queue = Collections.synchronizedList(mutableListOf<Entry>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Copying an apk into the system installer blocks, and the queue advances from the main thread
    protected val installScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Installer readiness. If false, queue check will not run.
     *
     * @see checkQueue
     */
    abstract var ready: Boolean

    /**
     * Add an item to install queue.
     *
     * @param downloadId Download ID as known by [ExtensionManager]
     * @param uri Uri of APK to install
     */
    fun addToQueue(
        downloadId: Long,
        uri: Uri,
        userActionBehavior: UserActionBehavior,
    ) {
        queue.add(Entry(downloadId, uri, userActionBehavior))
        checkQueue()
    }

    /**
     * Proceeds to install the APK of this entry inside this method. Call [continueQueue]
     * when the install process for this entry is finished to continue the queue.
     *
     * @param entry The [Entry] of item to process
     * @see continueQueue
     */
    @CallSuper
    open fun processEntry(entry: Entry) {
        extensionManager.setInstalling(entry.downloadId)
    }

    /**
     * Called before queue continues. Override this to handle when the removed entry is
     * currently being processed.
     *
     * @return true if this entry can be removed from queue.
     */
    open fun cancelEntry(entry: Entry): Boolean {
        return true
    }

    /**
     * Tells the queue to continue processing the next entry and updates the install step
     * of the completed entry ([waitingInstall]) to [ExtensionManager].
     *
     * @param resultStep new install step for the processed entry.
     * @see waitingInstall
     */
    fun continueQueue(resultStep: InstallStep) {
        val completedEntry = waitingInstall.exchange(null)
        if (completedEntry != null) {
            extensionManager.updateInstallStep(completedEntry.downloadId, resultStep)
            checkQueue()
        }
    }

    /**
     * Checks the queue. The provided service will be stopped if the queue is empty.
     * Will not be run when not ready.
     *
     * @see ready
     */
    fun checkQueue() {
        if (!ready) {
            return
        }
        val nextEntry = synchronized(queue) {
            if (queue.isEmpty()) {
                service.stopSelf()
                return
            }
            if (!waitingInstall.compareAndSet(null, queue.first())) return
            queue.removeAt(0)
        }
        processEntry(nextEntry)
    }

    /**
     * Call this method when the provided service is destroyed.
     */
    @CallSuper
    open fun onDestroy() {
        scope.cancel()
        installScope.cancel()
        val remaining = synchronized(queue) { queue.toList().also { queue.clear() } }
        remaining.forEach {
            extensionManager.updateInstallStep(
                it.downloadId,
                InstallStep.Error("The installer stopped before it got to this one"),
            )
        }
        waitingInstall.store(null)
    }

    protected fun getActiveEntry(): Entry? = waitingInstall.load()

    /**
     * Cancels queue for the provided download ID if exists.
     *
     * @param downloadId Download ID as known by [ExtensionManager]
     */
    private fun cancelQueue(downloadId: Long) {
        val waitingInstall = this.waitingInstall.load()
        // Every new install cancels its own package first, which must leave another package's active entry alone
        val toCancel = synchronized(queue) { queue.find { it.downloadId == downloadId } }
            ?: waitingInstall?.takeIf { it.downloadId == downloadId }
            ?: return
        if (cancelEntry(toCancel)) {
            queue.remove(toCancel)
            if (waitingInstall == toCancel) {
                // Currently processing removed entry, continue queue
                if (this.waitingInstall.compareAndSet(toCancel, null)) {
                    checkQueue()
                }
            }
            extensionManager.updateInstallStep(downloadId, InstallStep.Idle)
        }
    }

    /**
     * Install item to queue.
     *
     * @param downloadId Download ID as known by [ExtensionManager]
     * @param uri Uri of APK to install
     */
    data class Entry(
        val downloadId: Long,
        val uri: Uri,
        val userActionBehavior: UserActionBehavior,
    )

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            cancelRequests.collect { cancelQueue(it) }
        }
    }

    companion object {
        private val cancelRequests = MutableSharedFlow<Long>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        /**
         * Attempts to cancel the installation entry for the provided download ID.
         *
         * @param downloadId Download ID as known by [ExtensionManager]
         */
        fun cancelInstallQueue(downloadId: Long) {
            cancelRequests.tryEmit(downloadId)
        }
    }
}
