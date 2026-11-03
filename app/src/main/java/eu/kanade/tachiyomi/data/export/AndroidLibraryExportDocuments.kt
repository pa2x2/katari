package eu.kanade.tachiyomi.data.export

import android.app.Application
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import tachiyomi.domain.storage.DocumentUri

/** Writes through the content resolver, which opens the document the system picker returned. */
@Inject
class AndroidLibraryExportDocuments(private val application: Application) : LibraryExportDocuments {
    override fun write(document: DocumentUri, text: String) {
        application.contentResolver.openOutputStream(document.value.toUri())?.use { outputStream ->
            outputStream.write(text.toByteArray())
        }
    }
}
