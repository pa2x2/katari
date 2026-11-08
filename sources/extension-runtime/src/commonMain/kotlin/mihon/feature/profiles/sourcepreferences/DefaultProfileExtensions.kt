package mihon.feature.profiles.sourcepreferences

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManager
import mihon.feature.profiles.core.ProfileExtensions

/** The loaded extensions, whose settings are a shared-preferences file per extension and profile. */
@Inject
@ContributesBinding(AppScope::class)
class DefaultProfileExtensions(
    private val extensionManager: ExtensionManager,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
) : ProfileExtensions {
    override suspend fun installedSourceIds(): Set<Long> = extensionManager.getLoadedExtensions()
        .flatMap { extension -> extension.sources.map { source -> source.id } }
        .toSet()

    override fun deleteSettings(profileId: Long) = sourcePreferenceFiles.deleteProfile(profileId)
}
