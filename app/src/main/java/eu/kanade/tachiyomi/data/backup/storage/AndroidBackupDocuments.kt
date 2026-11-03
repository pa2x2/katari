package eu.kanade.tachiyomi.data.backup.storage

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.Inject
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import tachiyomi.domain.storage.DocumentUri
import java.io.FileOutputStream

@Inject
class AndroidBackupDocuments(private val context: Context) : BackupDocuments {
    override fun read(uri: DocumentUri): Source = context.contentResolver.openInputStream(uri.value.toUri())!!.source()

    override fun isWritableFile(uri: DocumentUri): Boolean = file(uri)?.isFile == true

    override fun write(uri: DocumentUri): Sink {
        return file(uri)!!.openOutputStream()
            .also {
                // Force overwrite old file
                (it as? FileOutputStream)?.channel?.truncate(0)
            }
            .sink()
    }

    override fun delete(uri: DocumentUri) {
        file(uri)?.delete()
    }

    private fun file(uri: DocumentUri): UniFile? = UniFile.fromUri(context, uri.value.toUri())
}
