package eu.kanade.tachiyomi.data.backup.create.creators

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupFileExtension
import eu.kanade.tachiyomi.extension.file.FileExtensionStorage
import eu.kanade.tachiyomi.extension.file.StoredFileExtension
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds

@Inject
class FileExtensionsBackupCreator(
    private val context: Context,
) {

    operator fun invoke(): List<BackupFileExtension> {
        val storage = FileExtensionStorage(context)
        return ExtensionKinds.fileKinds
            .flatMap(storage::listInstalled)
            .filterIsInstance<StoredFileExtension.Readable>()
            .map { stored ->
                val manifest = stored.extension.manifest
                BackupFileExtension(
                    pkgName = stored.extension.pkgName,
                    name = manifest.name,
                    storeIndexUrl = manifest.storeIndexUrl,
                    versionName = manifest.versionName,
                )
            }
    }
}
