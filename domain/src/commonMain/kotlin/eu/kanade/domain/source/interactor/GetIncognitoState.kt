package eu.kanade.domain.source.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.IncognitoPreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.SourceExtensionPackages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Inject
class GetIncognitoState(
    private val incognitoPreferences: IncognitoPreferences,
    private val sourcePreferences: SourcePreferences,
    private val extensionPackages: SourceExtensionPackages,
) {
    fun await(sourceId: Long?): Boolean {
        if (incognitoPreferences.incognitoMode.get()) return true
        if (sourceId == null) return false
        val extensionPackage = extensionPackages.packageName(sourceId)
            ?: return false

        return extensionPackage in sourcePreferences.incognitoExtensions.get()
    }

    fun subscribe(sourceId: Long?): Flow<Boolean> {
        if (sourceId == null) return incognitoPreferences.incognitoMode.changes()

        return combine(
            incognitoPreferences.incognitoMode.changes(),
            sourcePreferences.incognitoExtensions.changes(),
            extensionPackages.packageNameChanges(sourceId),
        ) { incognito, incognitoExtensions, extensionPackage ->
            incognito || (extensionPackage in incognitoExtensions)
        }
            .distinctUntilChanged()
    }
}
