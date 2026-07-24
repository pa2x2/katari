package eu.kanade.tachiyomi.data.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.entry.interactions.EntryConsumptionFeature
import mihon.entry.interactions.EntryDownloadActionFeature
import mihon.entry.interactions.EntryDownloadRuntimeFeature
import mihon.entry.interactions.EntryMergeNavigationFeature
import mihon.entry.interactions.EntryOpenFeature
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.core.common.Constants
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Global [BroadcastReceiver] that runs on UI thread
 * Pending Broadcasts should be made from here.
 * NOTE: Use local broadcasts if possible.
 */
class NotificationReceiver : BroadcastReceiver() {

    private val entryMergeNavigationFeature: EntryMergeNavigationFeature by injectLazy()
    private val entryRepository: EntryRepository by injectLazy()
    private val entryChapterRepository: EntryChapterRepository by injectLazy()
    private val entryConsumptionFeature: EntryConsumptionFeature by injectLazy()
    private val downloadRuntime: EntryDownloadRuntimeFeature by injectLazy()
    private val entryDownloadActionFeature: EntryDownloadActionFeature by injectLazy()
    private val entryOpenFeature: EntryOpenFeature by injectLazy()
    private val scope: CoroutineScope by injectLazy()
    private val entryActionHandler by lazy {
        NotificationEntryActionHandler(
            entryRepository = entryRepository,
            entryChapterRepository = entryChapterRepository,
            entryConsumptionFeature = entryConsumptionFeature,
            entryDownloadActionFeature = entryDownloadActionFeature,
            entryOpenFeature = entryOpenFeature,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Dismiss notification
            ACTION_DISMISS_NOTIFICATION -> dismissNotification(context, intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            // Resume the download service
            ACTION_RESUME_DOWNLOADS -> {
                downloadRuntime.start()
            }
            // Pause the download service
            ACTION_PAUSE_DOWNLOADS -> {
                downloadRuntime.pause()
            }
            // Clear the download queue
            ACTION_CLEAR_DOWNLOADS -> {
                downloadRuntime.clearQueue()
            }
            // Launch share activity and dismiss notification
            ACTION_SHARE_IMAGE ->
                shareImage(
                    context,
                    intent.getStringExtra(EXTRA_URI)!!.toUri(),
                )
            // Share backup file
            ACTION_SHARE_BACKUP ->
                shareFile(
                    context,
                    intent.getParcelableExtraCompat(EXTRA_URI)!!,
                    "application/x-protobuf+gzip",
                )
            ACTION_CANCEL_RESTORE -> cancelRestore(context)
            // Cancel library update and dismiss notification
            ACTION_CANCEL_LIBRARY_UPDATE -> cancelLibraryUpdate(context)
            // Start downloading app update
            ACTION_START_APP_UPDATE -> startDownloadAppUpdate(context, intent)
            // Cancel downloading app update
            ACTION_CANCEL_APP_UPDATE_DOWNLOAD -> cancelDownloadAppUpdate(context)
            ACTION_OPEN_EPISODE -> {
                openLegacyChild(context, intent.legacyEpisodeOpenChildPayload())
            }
            ACTION_OPEN_CHILD -> {
                val payload = intent.openChildPayload()
                if (payload != null) {
                    openChild(context, payload)
                } else {
                    openLegacyChild(context, intent.legacyGenericOpenChildPayload())
                }
            }
            ACTION_OPEN_CHAPTER -> {
                openLegacyChild(context, intent.legacyChapterOpenChildPayload())
            }
            ACTION_MARK_AS_READ -> {
                handleLegacyProfileAction(context, intent) {
                    dismissNotification(context, intent)
                    markConsumed(intent.legacyChapterUrlsPayload())
                }
            }
            ACTION_DOWNLOAD_CHAPTER -> {
                handleLegacyProfileAction(context, intent) {
                    dismissNotification(context, intent)
                    downloadChildren(intent.legacyChapterUrlsPayload())
                }
            }
            ACTION_MARK_CONSUMED -> {
                handleLegacyProfileAction(context, intent) {
                    dismissNotification(context, intent)
                    markConsumed(intent.childIdsPayload())
                }
            }
            ACTION_DOWNLOAD_CHILDREN -> {
                handleLegacyProfileAction(context, intent) {
                    dismissNotification(context, intent)
                    downloadChildren(intent.childIdsPayload())
                }
            }
            ACTION_MARK_AS_WATCHED -> {
                handleLegacyProfileAction(context, intent) {
                    dismissNotification(context, intent)
                    markConsumed(intent.legacyEpisodeIdsPayload())
                }
            }
        }
    }

    private fun openChild(context: Context, payload: OpenChildPayload?) {
        payload ?: return
        async(scope, Dispatchers.IO) {
            val opened = entryActionHandler.openChild(
                context,
                payload.profileId,
                payload.visibleEntryId,
                payload.ownerEntryId,
                payload.childId,
            )
            if (!opened) {
                withContext(Dispatchers.Main) {
                    context.toast(MR.strings.chapter_error)
                }
            }
        }
    }

    /**
     * Dismiss the notification
     *
     * @param notificationId the id of the notification
     */
    private fun dismissNotification(context: Context, notificationId: Int) {
        context.cancelNotification(notificationId)
    }

    /**
     * Called to start share intent to share image
     *
     * @param context context of application
     * @param uri path of file
     */
    private fun shareImage(context: Context, uri: Uri) {
        context.startActivity(uri.toShareIntent(context))
    }

    /**
     * Called to start share intent to share backup file
     *
     * @param context context of application
     * @param path path of file
     */
    private fun shareFile(context: Context, uri: Uri, fileMimeType: String) {
        context.startActivity(uri.toShareIntent(context, fileMimeType))
    }

    private fun openLegacyChild(context: Context, payload: LegacyOpenChildPayload?) {
        payload ?: return
        async(scope, Dispatchers.IO) {
            val destination = entryMergeNavigationFeature.resolveLegacyNotification(payload.ownerEntryId)
                ?: return@async
            context.startActivity(
                profileChildIntent(
                    context = context,
                    profileId = destination.requestedSubject.profileId,
                    visibleEntryId = payload.visibleEntryId ?: destination.visibleEntryId,
                    ownerEntryId = payload.ownerEntryId,
                    childId = payload.childId,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Method called when user wants to stop a backup restore job.
     *
     * @param context context of application
     */
    private fun cancelRestore(context: Context) {
        BackupRestoreJob.stop(context)
    }

    /**
     * Method called when user wants to stop a library update
     *
     * @param context context of application
     */
    private fun cancelLibraryUpdate(context: Context) {
        LibraryUpdateJob.stop(context)
    }

    private fun startDownloadAppUpdate(context: Context, intent: Intent) {
        val url = intent.getStringExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL) ?: return
        AppUpdateDownloadJob.start(context, url)
    }

    private fun cancelDownloadAppUpdate(context: Context) {
        AppUpdateDownloadJob.stop(context)
    }

    private fun dismissNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId > -1) {
            dismissNotification(context, notificationId, intent.getIntExtra(EXTRA_GROUP_ID, 0))
        }
    }

    private fun markConsumed(payload: ChildIdsPayload?) {
        payload ?: return
        markConsumed(payload.profileId, payload.entryId, payload.childIds)
    }

    private fun markConsumed(payload: ChildUrlsPayload?) {
        payload ?: return
        markConsumed(payload.profileId, payload.entryId, payload.childUrls)
    }

    private fun markConsumed(profileId: Long, entryId: Long, childIds: LongArray) {
        async(scope, Dispatchers.IO) {
            entryActionHandler.markConsumed(profileId, entryId, childIds)
        }
    }

    private fun markConsumed(profileId: Long, entryId: Long, childUrls: Array<String>) {
        async(scope, Dispatchers.IO) {
            entryActionHandler.markConsumed(profileId, entryId, childUrls)
        }
    }

    private fun downloadChildren(payload: ChildIdsPayload?) {
        payload ?: return
        downloadChildren(payload.profileId, payload.entryId, payload.childIds)
    }

    private fun downloadChildren(payload: ChildUrlsPayload?) {
        payload ?: return
        downloadChildren(payload.profileId, payload.entryId, payload.childUrls)
    }

    private fun downloadChildren(profileId: Long, entryId: Long, childIds: LongArray) {
        async(scope, Dispatchers.IO) {
            entryActionHandler.downloadChildren(profileId, entryId, childIds)
        }
    }

    private fun downloadChildren(profileId: Long, entryId: Long, childUrls: Array<String>) {
        async(scope, Dispatchers.IO) {
            entryActionHandler.downloadChildren(profileId, entryId, childUrls)
        }
    }

    private fun handleLegacyProfileAction(context: Context, intent: Intent, routedAction: () -> Unit) {
        if (intent.hasExtra(EXTRA_PROFILE_ID)) {
            routedAction()
            return
        }
        val entryId = when (intent.action) {
            ACTION_MARK_AS_WATCHED -> intent.getLongExtra(EXTRA_ANIME_ID, -1L)
            ACTION_MARK_AS_READ, ACTION_DOWNLOAD_CHAPTER -> intent.getLongExtra(EXTRA_MANGA_ID, -1L)
            ACTION_MARK_CONSUMED, ACTION_DOWNLOAD_CHILDREN -> intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
            else -> -1L
        }
        if (entryId <= -1L) return
        async(scope, Dispatchers.IO) {
            val destination = entryMergeNavigationFeature.resolveLegacyNotification(entryId) ?: return@async
            context.startActivity(
                profileNotificationIntent(
                    context = context,
                    profileId = destination.requestedSubject.profileId,
                    receiverAction = checkNotNull(intent.action),
                ).putExtras(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun BroadcastReceiver.async(
        scope: CoroutineScope,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        val result = goAsync()
        scope.launch(context, start) {
            try {
                block()
            } finally {
                result.finish()
            }
        }
    }

    private data class OpenChildPayload(
        val profileId: Long,
        val visibleEntryId: Long,
        val ownerEntryId: Long,
        val childId: Long,
    )

    private data class LegacyOpenChildPayload(
        val visibleEntryId: Long? = null,
        val ownerEntryId: Long,
        val childId: Long,
    )

    private data class ChildIdsPayload(
        val profileId: Long,
        val entryId: Long,
        val childIds: LongArray,
    )

    private data class ChildUrlsPayload(
        val profileId: Long,
        val entryId: Long,
        val childUrls: Array<String>,
    )

    private fun Intent.openChildPayload(): OpenChildPayload? {
        val profileId = getLongExtra(EXTRA_PROFILE_ID, -1)
        val visibleEntryId = getLongExtra(EXTRA_VISIBLE_ENTRY_ID, -1)
        val ownerEntryId = getLongExtra(EXTRA_OWNER_ENTRY_ID, visibleEntryId)
        val childId = getLongExtra(EXTRA_CHILD_ID, -1)
        return OpenChildPayload(profileId, visibleEntryId, ownerEntryId, childId).takeIf { it.isValid() }
    }

    private fun Intent.legacyEpisodeOpenChildPayload(): LegacyOpenChildPayload? {
        val visibleEntryId = getLongExtra(EXTRA_ANIME_ID, -1)
        val ownerEntryId = getLongExtra(EXTRA_OWNER_ANIME_ID, visibleEntryId)
        val childId = getLongExtra(EXTRA_EPISODE_ID, -1)
        return LegacyOpenChildPayload(visibleEntryId, ownerEntryId, childId).takeIf { it.isValid() }
    }

    private fun Intent.legacyChapterOpenChildPayload(): LegacyOpenChildPayload? {
        val ownerEntryId = getLongExtra(EXTRA_MANGA_ID, -1)
        val childId = getLongExtra(EXTRA_CHAPTER_ID, -1)
        return LegacyOpenChildPayload(ownerEntryId = ownerEntryId, childId = childId).takeIf { it.isValid() }
    }

    private fun Intent.legacyGenericOpenChildPayload(): LegacyOpenChildPayload? {
        val visibleEntryId = getLongExtra(EXTRA_VISIBLE_ENTRY_ID, -1L)
        val ownerEntryId = getLongExtra(EXTRA_OWNER_ENTRY_ID, visibleEntryId)
        val childId = getLongExtra(EXTRA_CHILD_ID, -1L)
        return LegacyOpenChildPayload(visibleEntryId, ownerEntryId, childId).takeIf { it.isValid() }
    }

    private fun Intent.childIdsPayload(): ChildIdsPayload? {
        val profileId = getLongExtra(EXTRA_PROFILE_ID, -1L)
        val entryId = getLongExtra(EXTRA_ENTRY_ID, -1)
        val childIds = getLongArrayExtra(EXTRA_CHILD_IDS) ?: return null
        return ChildIdsPayload(profileId, entryId, childIds).takeIf { it.isValid() }
    }

    private fun Intent.legacyEpisodeIdsPayload(): ChildIdsPayload? {
        val profileId = getLongExtra(EXTRA_PROFILE_ID, -1L)
        val entryId = getLongExtra(EXTRA_ANIME_ID, -1)
        val childIds = getLongArrayExtra(EXTRA_EPISODE_IDS) ?: return null
        return ChildIdsPayload(profileId, entryId, childIds).takeIf { it.isValid() }
    }

    private fun Intent.legacyChapterUrlsPayload(): ChildUrlsPayload? {
        val profileId = getLongExtra(EXTRA_PROFILE_ID, -1L)
        val entryId = getLongExtra(EXTRA_MANGA_ID, -1)
        val childUrls = getStringArrayExtra(EXTRA_CHAPTER_URL) ?: return null
        return ChildUrlsPayload(profileId, entryId, childUrls).takeIf { it.isValid() }
    }

    private fun OpenChildPayload.isValid(): Boolean {
        return profileId > -1 && visibleEntryId > -1 && ownerEntryId > -1 && childId > -1
    }

    private fun LegacyOpenChildPayload.isValid(): Boolean {
        return ownerEntryId > -1 && childId > -1
    }

    private fun ChildIdsPayload.isValid(): Boolean {
        return profileId > -1 && entryId > -1 && childIds.isNotEmpty()
    }

    private fun ChildUrlsPayload.isValid(): Boolean {
        return profileId > -1 && entryId > -1 && childUrls.isNotEmpty()
    }

    companion object {
        private const val NAME = "NotificationReceiver"

        private const val ACTION_SHARE_IMAGE = "$ID.$NAME.SHARE_IMAGE"

        private const val ACTION_SHARE_BACKUP = "$ID.$NAME.SEND_BACKUP"

        private const val ACTION_CANCEL_RESTORE = "$ID.$NAME.CANCEL_RESTORE"

        private const val ACTION_CANCEL_LIBRARY_UPDATE = "$ID.$NAME.CANCEL_LIBRARY_UPDATE"

        private const val ACTION_START_APP_UPDATE = "$ID.$NAME.ACTION_START_APP_UPDATE"
        private const val ACTION_CANCEL_APP_UPDATE_DOWNLOAD = "$ID.$NAME.CANCEL_APP_UPDATE_DOWNLOAD"

        private const val ACTION_OPEN_EPISODE = "$ID.$NAME.ACTION_OPEN_EPISODE"
        private const val ACTION_OPEN_CHILD = "$ID.$NAME.ACTION_OPEN_CHILD"
        private const val ACTION_ROUTE_PROFILE_NOTIFICATION = "$ID.$NAME.ACTION_ROUTE_PROFILE_NOTIFICATION"
        private const val ACTION_MARK_AS_READ = "$ID.$NAME.MARK_AS_READ"
        private const val ACTION_MARK_AS_WATCHED = "$ID.$NAME.MARK_AS_WATCHED"
        private const val ACTION_MARK_CONSUMED = "$ID.$NAME.MARK_CONSUMED"
        private const val ACTION_OPEN_CHAPTER = "$ID.$NAME.ACTION_OPEN_CHAPTER"
        private const val ACTION_DOWNLOAD_CHAPTER = "$ID.$NAME.ACTION_DOWNLOAD_CHAPTER"
        private const val ACTION_DOWNLOAD_CHILDREN = "$ID.$NAME.ACTION_DOWNLOAD_CHILDREN"

        private const val ACTION_RESUME_DOWNLOADS = "$ID.$NAME.ACTION_RESUME_DOWNLOADS"
        private const val ACTION_PAUSE_DOWNLOADS = "$ID.$NAME.ACTION_PAUSE_DOWNLOADS"
        private const val ACTION_CLEAR_DOWNLOADS = "$ID.$NAME.ACTION_CLEAR_DOWNLOADS"

        private const val ACTION_DISMISS_NOTIFICATION = "$ID.$NAME.ACTION_DISMISS_NOTIFICATION"

        private const val EXTRA_URI = "$ID.$NAME.URI"
        private const val EXTRA_NOTIFICATION_ID = "$ID.$NAME.NOTIFICATION_ID"
        private const val EXTRA_GROUP_ID = "$ID.$NAME.EXTRA_GROUP_ID"
        private const val EXTRA_ANIME_ID = "$ID.$NAME.EXTRA_ANIME_ID"
        private const val EXTRA_OWNER_ANIME_ID = "$ID.$NAME.EXTRA_OWNER_ANIME_ID"
        private const val EXTRA_EPISODE_ID = "$ID.$NAME.EXTRA_EPISODE_ID"
        private const val EXTRA_EPISODE_IDS = "$ID.$NAME.EXTRA_EPISODE_IDS"
        private const val EXTRA_MANGA_ID = "$ID.$NAME.EXTRA_MANGA_ID"
        private const val EXTRA_CHAPTER_ID = "$ID.$NAME.EXTRA_CHAPTER_ID"
        private const val EXTRA_CHAPTER_URL = "$ID.$NAME.EXTRA_CHAPTER_URL"
        private const val EXTRA_VISIBLE_ENTRY_ID = "$ID.$NAME.EXTRA_VISIBLE_ENTRY_ID"
        private const val EXTRA_OWNER_ENTRY_ID = "$ID.$NAME.EXTRA_OWNER_ENTRY_ID"
        private const val EXTRA_ENTRY_ID = "$ID.$NAME.EXTRA_ENTRY_ID"
        private const val EXTRA_CHILD_ID = "$ID.$NAME.EXTRA_CHILD_ID"
        private const val EXTRA_CHILD_IDS = "$ID.$NAME.EXTRA_CHILD_IDS"
        private const val EXTRA_PROFILE_ID = "$ID.$NAME.EXTRA_PROFILE_ID"
        private const val EXTRA_ROUTED_ACTION = "$ID.$NAME.EXTRA_ROUTED_ACTION"

        /**
         * Returns a [PendingIntent] that resumes the download of a chapter
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun resumeDownloadsPendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_RESUME_DOWNLOADS
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that pauses the download queue
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun pauseDownloadsPendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_PAUSE_DOWNLOADS
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns a [PendingIntent] that clears the download queue
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun clearDownloadsPendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CLEAR_DOWNLOADS
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that starts a service which dismissed the notification
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun dismissNotificationPendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that starts a service which dismissed the notification
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun dismissNotification(context: Context, notificationId: Int, groupId: Int? = null) {
            /*
            Group notifications always have at least 2 notifications:
            - Group summary notification
            - Single manga notification

            If the single notification is dismissed by the system, ie by a user swipe or tapping on the notification,
            it will auto dismiss the group notification if there's no other single updates.

            When programmatically dismissing this notification, the group notification is not automatically dismissed.
             */
            val groupKey = context.notificationManager.activeNotifications.find {
                it.id == notificationId
            }?.groupKey

            if (groupId != null && groupId != 0 && !groupKey.isNullOrEmpty()) {
                val notifications = context.notificationManager.activeNotifications.filter {
                    it.groupKey == groupKey
                }

                if (notifications.size == 2) {
                    context.cancelNotification(groupId)
                    return
                }
            }

            context.cancelNotification(notificationId)
        }

        /**
         * Returns [PendingIntent] that starts a share activity
         *
         * @param context context of application
         * @param uri location path of file
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun shareImagePendingBroadcast(context: Context, uri: Uri): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHARE_IMAGE
                putExtra(EXTRA_URI, uri.toString())
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun openChildPendingActivity(
            context: Context,
            visibleEntry: Entry,
            ownerEntry: Entry,
            chapter: EntryChapter,
        ): PendingIntent? {
            if (!Injekt.get<EntryOpenFeature>().isApplicable(visibleEntry.type)) return null
            require(visibleEntry.profileId == ownerEntry.profileId) {
                "Notification child destination and owner must belong to the same profile"
            }
            return PendingIntent.getActivity(
                context,
                chapter.id.hashCode(),
                profileChildIntent(
                    context = context,
                    profileId = visibleEntry.profileId,
                    visibleEntryId = visibleEntry.id,
                    ownerEntryId = ownerEntry.id,
                    childId = chapter.id,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun dispatchProfileNotificationIntent(context: Context, intent: Intent): Boolean {
            if (intent.action != ACTION_ROUTE_PROFILE_NOTIFICATION) return false
            val receiverAction = intent.getStringExtra(EXTRA_ROUTED_ACTION) ?: return false
            val broadcast = Intent(context, NotificationReceiver::class.java).apply {
                action = receiverAction
                putExtras(intent)
                putExtra(EXTRA_PROFILE_ID, intent.getLongExtra(Constants.PROFILE_EXTRA, -1L))
            }
            context.sendBroadcast(broadcast)
            return true
        }

        private fun profileChildIntent(
            context: Context,
            profileId: Long,
            visibleEntryId: Long,
            ownerEntryId: Long,
            childId: Long,
        ): Intent {
            return profileNotificationIntent(context, profileId, ACTION_OPEN_CHILD).apply {
                putExtra(EXTRA_VISIBLE_ENTRY_ID, visibleEntryId)
                putExtra(EXTRA_OWNER_ENTRY_ID, ownerEntryId)
                putExtra(EXTRA_CHILD_ID, childId)
            }
        }

        private fun profileNotificationIntent(
            context: Context,
            profileId: Long,
            receiverAction: String,
        ): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = ACTION_ROUTE_PROFILE_NOTIFICATION
                data = Uri.Builder()
                    .scheme("katari")
                    .authority("notification-action")
                    .appendPath(receiverAction)
                    .appendPath(profileId.toString())
                    .build()
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Constants.PROFILE_EXTRA, profileId)
                putExtra(EXTRA_ROUTED_ACTION, receiverAction)
            }
        }

        internal fun markConsumedPendingBroadcast(
            context: Context,
            entry: Entry,
            children: Array<EntryChapter>,
            groupId: Int,
        ): PendingIntent {
            val newIntent = profileNotificationIntent(context, entry.profileId, ACTION_MARK_CONSUMED).apply {
                putExtra(EXTRA_ENTRY_ID, entry.id)
                putExtra(EXTRA_CHILD_IDS, children.map { it.id }.toLongArray())
                putExtra(EXTRA_NOTIFICATION_ID, entry.id.hashCode())
                putExtra(EXTRA_GROUP_ID, groupId)
            }
            return PendingIntent.getActivity(
                context,
                entry.id.hashCode(),
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun downloadChildrenPendingBroadcast(
            context: Context,
            entry: Entry,
            children: Array<EntryChapter>,
            groupId: Int,
        ): PendingIntent {
            val newIntent = profileNotificationIntent(context, entry.profileId, ACTION_DOWNLOAD_CHILDREN).apply {
                putExtra(EXTRA_ENTRY_ID, entry.id)
                putExtra(EXTRA_CHILD_IDS, children.map { it.id }.toLongArray())
                putExtra(EXTRA_NOTIFICATION_ID, entry.id.hashCode())
                putExtra(EXTRA_GROUP_ID, groupId)
            }
            return PendingIntent.getActivity(
                context,
                entry.id.hashCode(),
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that opens the manga info controller
         *
         * @param context context of application
         * @param mangaId id of the entry to open
         */
        internal fun openEntryPendingActivity(
            context: Context,
            profileId: Long,
            entryId: Long,
            groupId: Int? = null,
        ): PendingIntent {
            val newIntent = Intent(context, MainActivity::class.java).setAction(Constants.SHORTCUT_MANGA)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.ENTRY_EXTRA, entryId)
                .putExtra(Constants.PROFILE_EXTRA, profileId)
                .putExtra("notificationId", entryId.hashCode())
            groupId?.let { newIntent.putExtra("groupId", it) }

            return PendingIntent.getActivity(
                context,
                entryId.hashCode(),
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that starts a service which stops the library update
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun cancelLibraryUpdatePendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_LIBRARY_UPDATE
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that starts the [AppUpdateDownloadJob] to download an app update.
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun downloadAppUpdatePendingBroadcast(
            context: Context,
            url: String,
            title: String? = null,
        ): PendingIntent {
            return Intent(context, NotificationReceiver::class.java).run {
                action = ACTION_START_APP_UPDATE
                putExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL, url)
                title?.let { putExtra(AppUpdateDownloadJob.EXTRA_DOWNLOAD_TITLE, it) }
                PendingIntent.getBroadcast(
                    context,
                    0,
                    this,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        }

        /**
         *
         */
        internal fun cancelDownloadAppUpdatePendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_APP_UPDATE_DOWNLOAD
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that opens the extensions screen.
         *
         * @param context context of application
         * @return [PendingIntent]
         */
        internal fun openExtensionsPendingActivity(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Constants.SHORTCUT_EXTENSIONS
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that directly launches a share activity for a backup file.
         *
         * @param context context of application
         * @param uri uri of backup file
         * @return [PendingIntent]
         */
        internal fun shareBackupPendingActivity(context: Context, uri: Uri): PendingIntent {
            val intent = uri.toShareIntent(context, "application/x-protobuf+gzip").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Returns [PendingIntent] that opens the error log file in an external viewer
         *
         * @param context context of application
         * @param uri uri of error log file
         * @return [PendingIntent]
         */
        internal fun openErrorLogPendingActivity(context: Context, uri: Uri): PendingIntent {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "text/plain")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        /**
         * Returns [PendingIntent] that cancels a backup restore job.
         *
         * @param context context of application
         * @param notificationId id of notification
         * @return [PendingIntent]
         */
        internal fun cancelRestorePendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_CANCEL_RESTORE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

internal class NotificationEntryActionHandler(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryConsumptionFeature: EntryConsumptionFeature,
    private val entryDownloadActionFeature: EntryDownloadActionFeature,
    private val entryOpenFeature: EntryOpenFeature,
) {
    suspend fun openChild(
        context: Context,
        profileId: Long,
        visibleEntryId: Long,
        ownerEntryId: Long,
        childId: Long,
    ): Boolean {
        if (visibleEntryId <= -1 || ownerEntryId <= -1 || childId <= -1) {
            return false
        }

        val entry = entryRepository.getEntryById(visibleEntryId, profileId) ?: return false
        val owner = entryRepository.getEntryById(ownerEntryId, profileId) ?: return false
        val chapter = entryChapterRepository.getChapterById(childId)
            ?.takeIf { it.entryId == owner.id }
            ?: return false

        return entryOpenFeature.open(
            context = context,
            entry = entry,
            chapter = chapter,
            options = EntryOpenOptions(
                ownerEntryId = ownerEntryId,
                newTask = true,
                clearTop = true,
            ),
        )
    }

    suspend fun markConsumed(profileId: Long, entryId: Long, childIds: LongArray) {
        val entry = entryRepository.getEntryById(entryId, profileId) ?: return
        val chapters = loadChapters(childIds)
        if (chapters.isNotEmpty()) {
            entryConsumptionFeature.setConsumed(entry, chapters, consumed = true)
        }
    }

    suspend fun markConsumed(profileId: Long, entryId: Long, childUrls: Array<String>) {
        val childIds = loadChildIds(entryId, childUrls)
        if (childIds.isNotEmpty()) {
            markConsumed(profileId, entryId, childIds)
        }
    }

    suspend fun downloadChildren(profileId: Long, entryId: Long, childIds: LongArray) {
        val entry = entryRepository.getEntryById(entryId, profileId) ?: return
        val chapters = loadChapters(childIds)
        if (chapters.isNotEmpty()) {
            entryDownloadActionFeature.download(
                entry = entry,
                chapters = chapters,
            )
        }
    }

    suspend fun downloadChildren(profileId: Long, entryId: Long, childUrls: Array<String>) {
        val childIds = loadChildIds(entryId, childUrls)
        if (childIds.isNotEmpty()) {
            downloadChildren(profileId, entryId, childIds)
        }
    }

    private suspend fun loadChapters(childIds: LongArray): List<EntryChapter> {
        return buildList {
            childIds.forEach { childId ->
                entryChapterRepository.getChapterById(childId)?.let(::add)
            }
        }
    }

    private suspend fun loadChildIds(entryId: Long, childUrls: Array<String>): LongArray {
        val urls = childUrls.toSet()
        return entryChapterRepository.getChaptersByEntryIdAwait(entryId)
            .filter { it.url in urls }
            .map { it.id }
            .toLongArray()
    }
}
