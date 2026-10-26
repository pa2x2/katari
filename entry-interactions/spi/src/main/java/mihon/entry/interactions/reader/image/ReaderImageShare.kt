package mihon.entry.interactions.reader.image

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** Opens the share sheet for a reader's picture, saved with [ReaderImageSaver], with [message] as its text. */
fun Uri.toReaderShareIntent(context: Context, type: String = "image/*", message: String? = null): Intent {
    val uri = this
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        when (uri.scheme) {
            "http", "https" -> putExtra(Intent.EXTRA_TEXT, uri.toString())
            "content" -> {
                message?.let { putExtra(Intent.EXTRA_TEXT, it) }
                putExtra(Intent.EXTRA_STREAM, uri)
            }
        }
        clipData = ClipData.newRawUri(null, uri)
        setType(type)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    return Intent.createChooser(shareIntent, context.stringResource(MR.strings.action_share)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
