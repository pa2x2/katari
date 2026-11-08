package app.katari.presentation.kit.files

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.storage.DocumentUri

@Composable
actual fun rememberDocumentPicker(
    request: DocumentPickerRequest,
    onResult: (DocumentPick?) -> Unit,
): DocumentPicker {
    val context = LocalContext.current
    val contract = remember(request) { request.contract() }
    val launcher = rememberLauncherForActivityResult(contract) { uri ->
        onResult(
            uri?.let {
                val access = request.accessToKeep
                DocumentPick(
                    uri = DocumentUri(it.toString()),
                    accessKept = access != null && context.keepAccess(it, access),
                )
            },
        )
    }
    return remember(launcher) { ActivityDocumentPicker(launcher) }
}

private class ActivityDocumentPicker(
    private val launcher: ManagedActivityResultLauncher<String?, Uri?>,
) : DocumentPicker {
    override fun launch(suggestedName: String?): Boolean = try {
        launcher.launch(suggestedName)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun Context.keepAccess(uri: Uri, access: DocumentAccess): Boolean {
    val flags = when (access) {
        DocumentAccess.Read -> Intent.FLAG_GRANT_READ_URI_PERMISSION
        DocumentAccess.ReadWrite -> Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
    return try {
        contentResolver.takePersistableUriPermission(uri, flags)
        true
    } catch (e: SecurityException) {
        logcat(LogPriority.WARN, e)
        false
    }
}
