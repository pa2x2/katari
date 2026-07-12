package eu.kanade.tachiyomi.ui.reader

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.ScaleDrawable
import mihon.entry.interactions.manga.R
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

/**
 * Class used to show BigPictureStyle notifications
 */
internal class SaveImageNotifier(private val context: Context) {

    private val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_COMMON)
    private val notificationId: Int = ID_DOWNLOAD_IMAGE

    /**
     * Called when image download/copy is complete.
     *
     * @param uri image file containing downloaded page image.
     */
    fun onComplete(uri: Uri) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .size(720, 1280)
            .target(
                onSuccess = { showCompleteNotification(uri, it.asDrawable(context.resources).getBitmapOrNull()) },
                onError = { onError(null) },
            )
            .build()
        context.imageLoader.enqueue(request)
    }

    /**
     * Clears the notification message.
     */
    fun onClear() {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Called on error while downloading image.
     * @param error string containing error information.
     */
    fun onError(error: String?) {
        // Create notification
        with(notificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.download_notifier_title_error))
            setContentText(error ?: context.stringResource(MR.strings.unknown_error))
            setSmallIcon(android.R.drawable.ic_menu_report_image)
        }
        updateNotification()
    }

    private fun showCompleteNotification(uri: Uri, image: Bitmap?) {
        with(notificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.picture_saved))
            setSmallIcon(R.drawable.ic_photo_24dp)
            image?.let { setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }
            setLargeIcon(image)
            setAutoCancel(true)

            // Clear old actions if they exist
            clearActions()

            setContentIntent(openImagePendingActivity(context, uri))
            // Share action
            addAction(
                R.drawable.ic_share_24dp,
                context.stringResource(MR.strings.action_share),
                shareImagePendingActivity(context, uri),
            )

            updateNotification()
        }
    }

    private fun updateNotification() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            return
        }

        // Displays the progress bar on notification
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }
}

private const val CHANNEL_COMMON = "common_channel"
private const val ID_DOWNLOAD_IMAGE = 2

private fun Drawable.getBitmapOrNull(): Bitmap? = when (this) {
    is BitmapDrawable -> bitmap
    is ScaleDrawable -> child.toBitmap()
    else -> null
}
