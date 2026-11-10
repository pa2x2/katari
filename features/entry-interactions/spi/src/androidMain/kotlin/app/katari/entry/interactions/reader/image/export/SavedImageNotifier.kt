package app.katari.entry.interactions.reader.image.export

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.drawable.toBitmap
import app.katari.entry.interactions.reader.image.toReaderShareIntent
import app.katari.notification.AppNotificationKind
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.ScaleDrawable
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import app.katari.icons.materialsymbols.R as MaterialSymbolsR

/** The notification for a saved image: the picture, opening it on a tap, with an action to share it. */
internal class SavedImageNotifier(private val context: Context) {

    private val notificationBuilder = NotificationCompat.Builder(context, AppNotificationKind.COMMON.id)
    private val notificationId: Int = ID_DOWNLOAD_IMAGE

    /**
     * Called when image download/copy is complete.
     *
     * @param uri the saved image.
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
            setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_photo)
            image?.let { setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }
            setLargeIcon(image)
            setAutoCancel(true)

            // Clear old actions if they exist
            clearActions()

            setContentIntent(openImagePendingActivity(context, uri))
            // Share action
            addAction(
                MaterialSymbolsR.drawable.rounded_share,
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

private const val ID_DOWNLOAD_IMAGE = 2

private fun openImagePendingActivity(context: Context, uri: Uri): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun shareImagePendingActivity(context: Context, uri: Uri): PendingIntent {
    return PendingIntent.getActivity(
        context,
        0,
        uri.toReaderShareIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun Drawable.getBitmapOrNull(): Bitmap? = when (this) {
    is BitmapDrawable -> bitmap
    is ScaleDrawable -> child.toBitmap()
    else -> null
}
