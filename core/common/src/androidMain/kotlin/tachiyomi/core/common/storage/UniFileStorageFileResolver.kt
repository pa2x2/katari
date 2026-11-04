package tachiyomi.core.common.storage

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile

class UniFileStorageFileResolver(private val context: Context) : StorageFileResolver {
    override fun fromUri(uri: String): StorageFile? =
        UniFile.fromUri(context, Uri.parse(uri))?.let { UniFileStorageFile(it, context) }
}
