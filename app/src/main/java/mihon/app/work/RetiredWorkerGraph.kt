package mihon.app.work

import androidx.work.ListenableWorker
import kotlin.reflect.KClass

interface RetiredWorkerGraph {
    /** Each retired worker's fully qualified class name, to the worker that now does its work. */
    val retiredWorkers: Map<String, KClass<out ListenableWorker>>
}
