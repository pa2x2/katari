package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mihon.entry.interactions.EntryDownloadInteraction
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

class AndroidSourceManager(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val sourceRepository: StubSourceRepository,
) : SourceManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val entryDownloadInteraction: EntryDownloadInteraction by injectLazy()

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMap = ConcurrentHashMap<Long, UnifiedSource>()
    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    private val _sources = MutableStateFlow<List<UnifiedSource>>(emptyList())
    override val sources: Flow<List<UnifiedSource>> = _sources.asStateFlow()

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    val extensionSources = mapUnifiedSources(extensions)
                    val localSource = LocalSource(
                        context,
                        Injekt.get(),
                        Injekt.get(),
                    )
                    val allSources = extensionSources + (localSource.id to localSource)

                    sourcesMap.clear()
                    sourcesMap.putAll(allSources)

                    _sources.value = allSources.values.toList()

                    extensionSources.values.forEach { source ->
                        registerStubSource(source)
                    }

                    _isInitialized.value = true
                }
        }

        scope.launch {
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = it
                    }
                    stubSourcesMap.clear()
                    stubSourcesMap.putAll(mutableMap)
                }
        }
    }

    override fun get(sourceKey: Long): UnifiedSource? {
        return sourcesMap[sourceKey]
    }

    override fun getOrStub(sourceKey: Long): UnifiedSource {
        return sourcesMap[sourceKey] ?: UnifiedStubSource(
            stubSourcesMap.getOrPut(sourceKey) {
                runBlocking { createStubSource(sourceKey) }
            },
        )
    }

    override fun getAll(): List<UnifiedSource> {
        return sourcesMap.values.toList()
    }

    override fun getCatalogueSources(): List<UnifiedSource> {
        return sourcesMap.values.filterIsInstance<EntryCatalogueSource>()
    }

    override fun getCatalogueSource(sourceKey: Long): EntryCatalogueSource? {
        return sourcesMap[sourceKey] as? EntryCatalogueSource
    }

    override fun getOnlineSources(): List<UnifiedSource> {
        return sourcesMap.values.filterIsInstance<SourceHomePage>()
    }

    override fun getStubSources(): List<UnifiedSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values
            .filterNot { it.id in onlineSourceIds }
            .map(::UnifiedStubSource)
    }

    override fun getDisplayInfo(sourceKey: Long): SourceDisplayInfo {
        val source = sourcesMap[sourceKey]
        if (source != null) {
            return SourceDisplayInfo(
                id = source.id,
                name = source.name,
                lang = (source as? EntryCatalogueSource)?.lang ?: "",
                isMissing = false,
            )
        }

        val stub = stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
        return SourceDisplayInfo(
            id = stub.id,
            name = stub.name.ifBlank { stub.id.toString() },
            lang = stub.lang,
            isMissing = true,
        )
    }

    private fun registerStubSource(source: UnifiedSource) {
        scope.launch {
            val stub = StubSource(
                id = source.id,
                lang = (source as? EntryCatalogueSource)?.lang ?: "",
                name = source.name,
            )
            val dbSource = sourceRepository.getStubSource(stub.id)
            if (dbSource == stub) return@launch
            sourceRepository.upsertStubSource(stub.id, stub.lang, stub.name)
            if (dbSource != null) {
                entryDownloadInteraction.renameSource(UnifiedStubSource(dbSource), UnifiedStubSource(stub))
            }
        }
    }

    private suspend fun createStubSource(id: Long): StubSource {
        sourceRepository.getStubSource(id)?.let {
            return it
        }
        extensionManager.getSourceData(id)?.let {
            return it
        }
        return StubSource(id = id, lang = "", name = "")
    }
}

private fun mapUnifiedSources(
    extensions: List<Extension.Installed>,
): Map<Long, UnifiedSource> {
    val map = ConcurrentHashMap<Long, UnifiedSource>()

    extensions.forEach { extension ->
        extension.sources.forEach { unified ->
            map[unified.id] = unified
        }
    }

    return map
}
