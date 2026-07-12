package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.interactor.GetEnabledCatalogSources
import eu.kanade.domain.source.model.BUILTIN_LATEST_PRESET_ID
import eu.kanade.domain.source.model.BUILTIN_POPULAR_PRESET_ID
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.latestFeedPreset
import eu.kanade.domain.source.model.popularFeedPreset
import eu.kanade.domain.source.model.resolvedDisplayMode
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.UUID

class FeedsScreenModel(
    private val contentMode: SourceFeedContentMode = SourceFeedContentMode.Browse,
    private val browseFeedService: BrowseFeedService = Injekt.get(),
    private val profileSourcePreferences: ProfileSourcePreferences = Injekt.get(),
    private val getEnabledCatalogSources: GetEnabledCatalogSources = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
) : StateScreenModel<FeedsScreenModel.State>(State()) {

    init {
        screenModelScope.launchIO {
            observeProfileAwareFeedState(
                activeProfileIdFlow = activeProfileProvider.activeProfileIdFlow,
                enabledSources = { getEnabledCatalogSources.subscribe(it) },
                browseState = { browseFeedService.forProfile(it).state() },
                sourcesLoaded = sourceManager.isInitialized,
                contentMode = contentMode,
            ).collectLatest { observedState ->
                mutableState.update { state ->
                    val nextState = state.copy(
                        profileId = observedState.profileId,
                        sources = observedState.sources,
                        presets = observedState.presets,
                        feeds = observedState.feeds,
                        sourcesLoaded = observedState.sourcesLoaded,
                    )
                    val nextDialog = when {
                        nextState.validFeeds.isEmpty() && state.dialog == Dialog.ManageFeeds -> null
                        else -> nextState.dialog
                    }

                    nextState.copy(
                        selectedFeedId = resolveSelectedFeedId(
                            requestedId = observedState.selectedFeedId,
                            state = nextState,
                        ),
                        dialog = nextDialog,
                    )
                }

                pruneInvalidFeedsIfReady()
            }
        }
    }

    fun showCreateDialog() {
        mutableState.update { it.copy(dialog = Dialog.SelectSource) }
    }

    fun showManageDialog() {
        mutableState.update {
            if (it.validFeeds.isEmpty()) {
                it.copy(dialog = null)
            } else {
                it.copy(dialog = Dialog.ManageFeeds)
            }
        }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun selectSource(source: Source) {
        mutableState.update { it.copy(dialog = Dialog.SelectPreset(source.id)) }
    }

    fun selectFeed(feedId: String) {
        persistSelectedFeed(feedId)
        mutableState.update { it.copy(selectedFeedId = feedId) }
    }

    fun createFeed(sourceId: Long, presetId: String) {
        val browseFeedService = profileBrowseFeedService()
        val existing = state.value.feeds.firstOrNull {
            it.sourceId == sourceId && it.presetId == presetId
        }
        if (existing != null) {
            browseFeedService.updateFeed(existing.copy(enabled = true))
            browseFeedService.selectFeed(existing.id)
            closeDialog()
            return
        }

        browseFeedService.createFeed(
            SourceFeed(
                id = UUID.randomUUID().toString(),
                contentMode = contentMode,
                sourceId = sourceId,
                presetId = presetId,
                enabled = true,
                displayMode = sourcePreferences().sourceDisplayMode(sourceId).get().serialize(),
            ),
        )
        closeDialog()
    }

    fun toggleFeed(feedId: String, enabled: Boolean) {
        val feed = state.value.feeds.firstOrNull { it.id == feedId } ?: return
        profileBrowseFeedService().updateFeed(feed.copy(enabled = enabled))
    }

    fun updateFeedDisplayMode(feedId: String, displayMode: LibraryDisplayMode) {
        val feed = state.value.feeds.firstOrNull { it.id == feedId } ?: return
        profileBrowseFeedService().updateFeed(feed.copy(displayMode = displayMode.serialize()))
    }

    fun displayModeFor(feed: SourceFeed, defaultDisplayMode: LibraryDisplayMode): LibraryDisplayMode {
        return feed.resolvedDisplayMode(defaultDisplayMode)
    }

    fun sourceDisplayMode(sourceId: Long): LibraryDisplayMode {
        return sourcePreferences().sourceDisplayMode(sourceId).get()
    }

    fun removeFeed(feedId: String) {
        profileBrowseFeedService().removeFeed(feedId)
    }

    fun reorderFeed(fromFeedId: String, toFeedId: String) {
        val browseFeedService = profileBrowseFeedService()
        val allFeeds = browseFeedService.stateSnapshot().feeds
        val fromIndex = allFeeds.indexOfFirst { it.id == fromFeedId }
        val toIndex = allFeeds.indexOfFirst { it.id == toFeedId }
        if (fromIndex == -1 || toIndex == -1) return
        browseFeedService.reorderFeed(fromIndex, toIndex)
    }

    fun presetsFor(source: Source): List<SourceFeedPreset> {
        val builtin = buildList {
            add(popularFeedPreset(source.id, "Popular"))
            if (source.supportsLatest) {
                add(latestFeedPreset(source.id, "Latest"))
            }
        }
        val custom = state.value.presets.filter { it.sourceId == source.id }
        return builtin + custom
    }

    fun activeFeed(): SourceFeed? {
        val enabledFeeds = state.value.enabledFeeds
        return enabledFeeds.firstOrNull { it.id == state.value.selectedFeedId }
            ?: enabledFeeds.firstOrNull()
    }

    fun presetFor(feed: SourceFeed): SourceFeedPreset? {
        val source = state.value.sources.firstOrNull { it.id == feed.sourceId } ?: return null
        return when (feed.presetId) {
            BUILTIN_POPULAR_PRESET_ID -> popularFeedPreset(source.id, "Popular")
            BUILTIN_LATEST_PRESET_ID ->
                source
                    .takeIf(Source::supportsLatest)
                    ?.let { latestFeedPreset(it.id, "Latest") }

            else -> state.value.presets.firstOrNull {
                it.id == feed.presetId && it.sourceId == source.id
            }
        }
    }

    fun sourceFor(sourceId: Long): Source? {
        return state.value.sources.firstOrNull { it.id == sourceId }
    }

    private fun resolveSelectedFeedId(requestedId: String?, state: State): String? {
        val enabledFeeds = state.enabledFeeds
        return when {
            enabledFeeds.isEmpty() -> null
            requestedId != null && enabledFeeds.any { it.id == requestedId } -> requestedId
            else -> enabledFeeds.first().id.also { persistSelectedFeed(it, state.profileId) }
        }
    }

    private fun persistSelectedFeed(feedId: String, profileId: Long? = state.value.profileId) {
        val browseFeedService = browseFeedService.forProfile(profileId ?: activeProfileProvider.activeProfileId)
        when (contentMode) {
            SourceFeedContentMode.Browse -> browseFeedService.selectFeed(feedId)
            SourceFeedContentMode.Video -> browseFeedService.selectVideoFeed(feedId)
        }
    }

    private fun pruneInvalidFeedsIfReady() {
        val currentState = state.value
        if (!currentState.sourcesLoaded) return

        val browseFeedService = browseFeedService.forProfile(currentState.profileId ?: return)
        currentState.feeds
            .filterNot(currentState::isFeedValid)
            .forEach { browseFeedService.removeFeed(it.id) }
    }

    private fun profileBrowseFeedService(): BrowseFeedService {
        return browseFeedService.forProfile(state.value.profileId ?: activeProfileProvider.activeProfileId)
    }

    private fun sourcePreferences(): SourcePreferences {
        return profileSourcePreferences.forProfile(state.value.profileId ?: activeProfileProvider.activeProfileId)
    }

    sealed interface Dialog {
        data object SelectSource : Dialog
        data class SelectPreset(val sourceId: Long) : Dialog
        data object ManageFeeds : Dialog
    }

    @Immutable
    data class State(
        val profileId: Long? = null,
        val sources: ImmutableList<Source> = persistentListOf(),
        val presets: ImmutableList<SourceFeedPreset> = persistentListOf(),
        val feeds: ImmutableList<SourceFeed> = persistentListOf(),
        val sourcesLoaded: Boolean = false,
        val selectedFeedId: String? = null,
        val dialog: Dialog? = null,
    ) {
        fun isFeedValid(feed: SourceFeed): Boolean {
            if (!sourcesLoaded) return true

            val source = sources.firstOrNull { it.id == feed.sourceId } ?: return false
            return when (feed.presetId) {
                BUILTIN_POPULAR_PRESET_ID -> true
                BUILTIN_LATEST_PRESET_ID -> source.supportsLatest
                else -> presets.any { it.id == feed.presetId && it.sourceId == source.id }
            }
        }

        val validFeeds: ImmutableList<SourceFeed>
            get() = feeds.filter(::isFeedValid).toImmutableList()

        val enabledFeeds: ImmutableList<SourceFeed>
            get() = validFeeds.filter { it.enabled }.toImmutableList()
    }
}

internal fun observeProfileAwareFeedState(
    activeProfileIdFlow: Flow<Long>,
    enabledSources: (Long) -> Flow<List<Source>>,
    browseState: (Long) -> Flow<BrowseFeedService.State>,
    sourcesLoaded: Flow<Boolean>,
    contentMode: SourceFeedContentMode = SourceFeedContentMode.Browse,
): Flow<FeedsScreenModel.State> {
    return activeProfileIdFlow
        .distinctUntilChanged()
        .flatMapLatest { profileId ->
            combine(
                enabledSources(profileId),
                browseState(profileId),
                sourcesLoaded,
            ) { sources, browseState, sourcesLoaded ->
                FeedsScreenModel.State(
                    profileId = profileId,
                    sources = sources
                        .groupBy { it.id }
                        .values
                        .map { entries ->
                            entries.firstOrNull { !it.isUsedLast } ?: entries.first()
                        }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                        .toImmutableList(),
                    presets = browseState.presets.toImmutableList(),
                    feeds = browseState.feeds
                        .filter { it.contentMode == contentMode }
                        .toImmutableList(),
                    sourcesLoaded = sourcesLoaded,
                    selectedFeedId = when (contentMode) {
                        SourceFeedContentMode.Browse -> browseState.selectedFeedId
                        SourceFeedContentMode.Video -> browseState.selectedVideoFeedId
                    },
                )
            }
        }
}
