package eu.kanade.domain.extension.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class GetExtensionLanguages(
    private val preferences: SourcePreferences,
    private val extensions: ExtensionManagement,
) {
    fun subscribe(): Flow<List<String>> {
        return combine(
            preferences.enabledLanguages.changes(),
            extensions.available,
        ) { enabledLanguage, availableExtensions ->
            availableExtensions
                .flatMap { ext ->
                    ext.sources.map { it.lang }
                }
                .distinct()
                .sortedWith(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
