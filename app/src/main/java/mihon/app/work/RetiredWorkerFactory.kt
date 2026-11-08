package mihon.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import kotlin.reflect.KClass

/**
 * Creates the workers WorkManager asks for by a class name the app no longer has. WorkManager stores each request's
 * worker class name and creates the worker from it, after app updates too, so work queued before its worker was renamed
 * or replaced would otherwise fail. [retiredWorkers] maps each such name to the worker that now does that work. Any
 * other name is left to WorkManager's own factory.
 */
class RetiredWorkerFactory(
    private val retiredWorkers: () -> Map<String, KClass<out ListenableWorker>>,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        val worker = retiredWorkers()[workerClassName] ?: return null
        return worker.java
            .getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
            .newInstance(appContext, workerParameters)
    }
}
