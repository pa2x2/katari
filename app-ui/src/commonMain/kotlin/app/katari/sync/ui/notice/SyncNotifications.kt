package app.katari.sync.ui.notice

import app.katari.core.common.platform.PlatformFacility
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.trigger.SyncBackgroundWork
import okhttp3.HttpUrl

/**
 * Each platform's notifications for sync (D028): questions waiting and sync having stopped, each shown in place of the
 * one before it. Tapping a question notification opens the questions, the other the Sync page. Progress posts none,
 * except a long run's, which the platform's work for it shows ([SyncBackgroundWork.longRun]).
 */
@PlatformFacility
interface SyncNotifications {
    fun showQuestions(count: Int)

    fun withdrawQuestions()

    /** [server] is the server synced with, null when it isn't known. */
    fun showStopped(reason: SyncStop, server: HttpUrl?)

    fun withdrawStopped()
}
