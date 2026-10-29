package eu.kanade.tachiyomi.extension

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.domain.extension.model.ContentWarning
import tachiyomi.domain.source.service.NsfwSourceIds

/** The sources of loaded extensions whose content warning isn't [ContentWarning.SAFE]. */
@Inject
@ContributesBinding(AppScope::class)
class ExtensionNsfwSourceIds(private val extensions: ExtensionManagement) : NsfwSourceIds {
    override fun subscribe(): Flow<Set<Long>> = extensions.loaded
        .map { extensions ->
            extensions
                .filter { it.contentWarning != ContentWarning.SAFE }
                .flatMapTo(HashSet()) { ext -> ext.sources.map { it.id } }
        }
        .distinctUntilChanged()
}
