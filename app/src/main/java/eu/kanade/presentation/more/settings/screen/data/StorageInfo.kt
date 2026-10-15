package eu.kanade.presentation.more.settings.screen.data

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

@Composable
fun StorageInfo(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val storages by produceState(emptyList<File>()) {
        value = withContext(Dispatchers.IO) { DiskUtil.getExternalStorages(context) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        storages.forEach {
            StorageInfo(it)
        }
    }
}

@Composable
private fun StorageInfo(
    file: File,
) {
    val context = LocalContext.current

    val space by produceState(0L to 0L, file) {
        value = withContext(Dispatchers.IO) {
            DiskUtil.getAvailableStorageSpace(file) to DiskUtil.getTotalStorageSpace(file)
        }
    }
    val (available, total) = space
    val availableText = remember(available) { Formatter.formatFileSize(context, available) }
    val totalText = remember(total) { Formatter.formatFileSize(context, total) }

    Column(
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        Text(
            text = file.absolutePath,
            role = TextRole.Body,
            tone = ContentTone.Secondary,
        )

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { if (total > 0) 1 - (available / total.toFloat()) else 0f },
        )

        Text(
            text = stringResource(MR.strings.available_disk_space_info, availableText, totalText),
            role = TextRole.Supporting,
        )
    }
}
