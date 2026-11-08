package eu.kanade.tachiyomi.data.backup.extensions

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupFileExtension
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.file.FileExtensionStorage
import eu.kanade.tachiyomi.extension.file.RestoredFileExtension
import eu.kanade.tachiyomi.extension.file.StoredFileExtension
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles

@Inject
@ContributesBinding(AppScope::class)
class DefaultBackedUpFileExtensions(
    private val files: ExtensionFiles,
    private val kinds: ExtensionKinds,
    private val extensionManager: ExtensionManager,
) : BackedUpFileExtensions {

    override fun installed(): List<BackupFileExtension> {
        val storage = FileExtensionStorage(files)
        return kinds.fileKinds
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

    override fun reinstall(extensions: List<BackupFileExtension>) {
        extensionManager.installFromBackup(
            extensions.map { RestoredFileExtension(it.pkgName, it.name, it.storeIndexUrl) },
        )
    }
}
