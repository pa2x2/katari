package mihon.desktop.source

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager

/**
 * Sources as desktop has them: none installed, so every source an entry names is a stub that can't load anything,
 * shown by its ID as on Android before the source's name is known. Stands in until desktop loads extensions.
 */
@Inject
class NoSourceManager : SourceManager {
    override val isInitialized: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override val sources: Flow<List<UnifiedSource>> = flowOf(emptyList())

    override fun get(sourceKey: Long): UnifiedSource? = null

    override fun getOrStub(sourceKey: Long): UnifiedSource = UnifiedStubSource(id = sourceKey, lang = "", name = "")

    override fun getAll(): List<UnifiedSource> = emptyList()

    override fun getStubSources(): List<UnifiedSource> = emptyList()

    override fun getDisplayInfo(sourceKey: Long): SourceDisplayInfo = SourceDisplayInfo(
        id = sourceKey,
        name = sourceKey.toString(),
        lang = "",
        isMissing = true,
        hasKnownName = false,
    )
}
