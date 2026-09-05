package mihon.entry.interactions.download

import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [29])
class EntryDownloadWorkControllerTest {
    private val workers = mutableListOf<ControlledDownloadWorker>()
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var controller: DefaultEntryDownloadWorkController

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setTaskExecutor(SynchronousExecutor())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker {
                    check(workerClassName == EntryDownloadJob::class.java.name)
                    return ControlledDownloadWorker(appContext, workerParameters).also(workers::add)
                }
            })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
        controller = DefaultEntryDownloadWorkController(context)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get()
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun `start arriving after the drain returns survives the finishing worker`() {
        controller.start()
        allowScheduledWork()
        workers.size shouldBe 1
        val finishing = workers.single()

        // Processing has drained, but WorkManager still owns the RUNNING worker until its result arrives.
        controller.start()
        finishing.isStopped shouldBe false
        workers.size shouldBe 1
        finishing.result.set(ListenableWorker.Result.success())
        allowScheduledWork()

        workers.size shouldBe 2
        workers.last().result.set(ListenableWorker.Result.success())
        workManager.getWorkInfosForUniqueWork("EntryDownloader").get().all { it.state.isFinished } shouldBe true
    }

    @Test
    fun `pause cancels active and successor work and an explicit start can resume`() {
        controller.start()
        allowScheduledWork()
        controller.start()

        controller.stop()

        workers.single().isStopped shouldBe true
        workManager.getWorkInfosForUniqueWork("EntryDownloader").get().all { it.state.isFinished } shouldBe true
        controller.resumeIfRequested()
        workers.size shouldBe 1

        controller.start()
        allowScheduledWork()
        workers.size shouldBe 2
        workers.last().result.set(ListenableWorker.Result.success())
    }

    private fun allowScheduledWork() {
        val driver = checkNotNull(WorkManagerTestInitHelper.getTestDriver(context))
        workManager.getWorkInfosForUniqueWork("EntryDownloader").get()
            .filter { !it.state.isFinished }
            .forEach { driver.setAllConstraintsMet(it.id) }
    }
}
