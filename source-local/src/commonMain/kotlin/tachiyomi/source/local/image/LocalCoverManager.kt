package tachiyomi.source.local.image

import com.hippo.unifile.UniFile
import tachiyomi.source.local.LocalEntryMetadata
import java.io.InputStream

expect class LocalCoverManager {

    fun find(mangaUrl: String): UniFile?

    internal fun update(entry: LocalEntryMetadata, inputStream: InputStream): UniFile?

    fun update(mangaUrl: String, inputStream: InputStream): UniFile?
}
