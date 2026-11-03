package eu.kanade.tachiyomi.crash

import android.content.Context
import android.content.Intent

/** Opens [CrashActivity], which has a process of its own, as the only activity of a new task. */
class ActivityCrashPresenter(private val applicationContext: Context) : CrashPresenter {
    override fun present(report: String) {
        val intent = Intent(applicationContext, CrashActivity::class.java).apply {
            putExtra(INTENT_EXTRA, report)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        applicationContext.startActivity(intent)
    }

    companion object {
        private const val INTENT_EXTRA = "Throwable"

        fun exceptionFrom(intent: Intent): Throwable? = UncaughtCrashHandling.decode(
            intent.getStringExtra(INTENT_EXTRA),
        )
    }
}
