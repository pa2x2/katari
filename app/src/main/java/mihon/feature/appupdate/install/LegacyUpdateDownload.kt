package mihon.feature.appupdate.install

import android.content.Context
import java.io.File

/** Deletes the files the WorkManager download of Katari 1.12.0 and earlier left in the external cache. */
internal fun removeLegacyUpdateDownload(context: Context) {
    val directory = context.externalCacheDir ?: context.cacheDir
    directory.listFiles { file ->
        file.name == "update.apk" ||
            file.name == "update.url" ||
            (file.name.startsWith("update-") && file.name.endsWith(".part"))
    }?.forEach(File::delete)
}
