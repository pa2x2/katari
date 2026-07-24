package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.model.SourceDisplayInfo

/**
 * Central registry that exposes every installed source through the unified
 * [UnifiedSource] contract.
 *
 * Legacy manga sources are transparently wrapped by the adapter layer, so
 * callers never need to distinguish between legacy and new implementations.
 */
interface SourceManager {

    val isInitialized: StateFlow<Boolean>

    val sources: Flow<List<UnifiedSource>>

    fun get(sourceKey: Long): UnifiedSource?

    fun getOrStub(sourceKey: Long): UnifiedSource

    fun getAll(): List<UnifiedSource>

    fun getStubSources(): List<UnifiedSource>

    fun getDisplayInfo(sourceKey: Long): SourceDisplayInfo
}
