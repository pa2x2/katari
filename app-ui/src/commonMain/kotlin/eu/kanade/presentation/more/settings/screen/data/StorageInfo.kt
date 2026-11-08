package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.ui.di.LocalAppUiGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.fileSize
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

@Composable
fun StorageInfo(
    modifier: Modifier = Modifier,
) {
    val graph = LocalAppUiGraph.current
    val volumes = remember(graph) { graph.storageVolumes }
    val storages by produceState(emptyList<File>(), volumes) {
        value = withContext(Dispatchers.IO) { volumes.roots() }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        storages.forEach {
            StorageInfo(volumes, it)
        }
    }
}

@Composable
private fun StorageInfo(
    volumes: StorageVolumes,
    file: File,
) {
    val space by produceState(StorageSpace(availableBytes = 0, totalBytes = 0), file) {
        value = withContext(Dispatchers.IO) { volumes.space(file) }
    }
    val available = space.availableBytes
    val total = space.totalBytes
    val availableText = fileSize(available)
    val totalText = fileSize(total)

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
