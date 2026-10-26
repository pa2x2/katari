package eu.kanade.domain.extension.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.store.isServedBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mihon.domain.extension.model.ExtensionStore

@Inject
class GetExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> {
        return combine(
            preferences.enabledLanguages.changes(),
            preferences.enabledContentWarnings.changes(),
            extensionManager.loadedExtensionsFlow,
            extensionManager.notLoadedExtensionsFlow,
            extensionManager.availableExtensionsFlow,
        ) { enabledLanguages, enabledContentWarnings, _loaded, _notLoaded, _available ->
            val byName = compareBy<Extension, String>(String.CASE_INSENSITIVE_ORDER) { it.name }

            val (loadedUpdates, loaded) = _loaded.partition { it.hasUpdate }
            val (notLoadedUpdates, notLoaded) = _notLoaded.partition { it.hasUpdate }

            val updates = (loadedUpdates + notLoadedUpdates).sortedWith(byName)

            // A listing from a store with another signing key is a different apk, and another unsigned store's is
            // another file, either of which stays on offer. One whose store's key isn't known yet can't be told apart
            // from the installed apk, so it's taken for it.
            val installed = (_loaded + _notLoaded).groupBy { it.pkgName }
            val available = _available
                .filter { extension ->
                    installed[extension.pkgName].orEmpty().none {
                        extension.store.signingKey == ExtensionStore.NO_SIGNING_KEY || it.isServedBy(extension.store)
                    } &&
                        extension.contentWarning in enabledContentWarnings
                }
                // A row per source, each still installing its extension by the package name it installs as
                .flatMap { ext ->
                    ext.sources.filter { it.lang in enabledLanguages }
                        .map {
                            ext.copy(
                                name = it.name,
                                lang = it.lang,
                                sources = listOf(it),
                            )
                        }
                }
                .sortedWith(
                    compareBy<Extension.Available, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
                        .thenBy { it.store.signingKey },
                )

            Extensions(
                updates = updates,
                loaded = loaded.sortedWith(compareBy<Extension.Loaded> { !it.isObsolete }.then(byName)),
                available = available,
                notLoaded = notLoaded.sortedWith(byName),
            )
        }
    }
}
