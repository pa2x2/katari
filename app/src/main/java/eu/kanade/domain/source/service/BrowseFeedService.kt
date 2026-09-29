package eu.kanade.domain.source.service

import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.SourceFeedTimeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private val presetMutationLock = Any()

class BrowseFeedService(
    private val preferences: SourcePreferences,
    private val profilePreferences: ProfileSourcePreferences? = null,
) {

    fun forProfile(profileId: Long): BrowseFeedService {
        val preferences = profilePreferences?.forProfile(profileId) ?: return this
        return BrowseFeedService(preferences)
    }

    fun stateSnapshot(): State {
        return State(
            presets = preferences.savedFeedPresets.get(),
            feeds = preferences.savedFeeds.get(),
            selectedFeedId = preferences.selectedFeedId.get().takeIf { it.isNotBlank() },
            selectedVideoFeedId = preferences.selectedVideoFeedId.get().takeIf { it.isNotBlank() },
        )
    }

    fun presets(): Flow<List<SourceFeedPreset>> {
        return preferences.savedFeedPresets.changes()
    }

    fun feeds(): Flow<List<SourceFeed>> {
        return preferences.savedFeeds.changes()
    }

    fun state(): Flow<State> {
        return combine(
            presets(),
            feeds(),
            preferences.selectedFeedId.changes(),
            preferences.selectedVideoFeedId.changes(),
        ) { presets, feeds, selectedFeedId, selectedVideoFeedId ->
            State(
                presets = presets,
                feeds = feeds,
                selectedFeedId = selectedFeedId.takeIf { it.isNotBlank() },
                selectedVideoFeedId = selectedVideoFeedId.takeIf { it.isNotBlank() },
            )
        }
    }

    fun migratePresetFilters(original: SourceFeedPreset, filters: List<eu.kanade.domain.source.model.FilterStateNode>) {
        synchronized(presetMutationLock) {
            if (original.filters == filters) return
            val current = preferences.savedFeedPresets.get()
            if (current.firstOrNull { it.id == original.id } != original) return
            preferences.savedFeedPresets.set(
                current.map { if (it.id == original.id) it.copy(filters = filters) else it },
            )
        }
    }

    fun savePreset(preset: SourceFeedPreset) {
        synchronized(presetMutationLock) {
            val existingPresets = preferences.savedFeedPresets.get()
            val previousPreset = existingPresets.firstOrNull { it.id == preset.id }
            preferences.savedFeedPresets.set(
                existingPresets
                    .filterNot { it.id == preset.id }
                    .plus(preset)
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
            )

            if (previousPreset != null && previousPreset.feedBehaviorChanged(preset)) {
                preferences.savedFeeds.get()
                    .filter { it.presetId == preset.id }
                    .map(SourceFeed::id)
                    .forEach(::clearTimeline)
            }
        }
    }

    fun removePreset(presetId: String) {
        synchronized(presetMutationLock) {
            preferences.savedFeedPresets.set(
                preferences.savedFeedPresets.get().filterNot { it.id == presetId },
            )

            val removedFeedIds = preferences.savedFeeds.get()
                .filter { it.presetId == presetId }
                .map(SourceFeed::id)
            val remainingFeeds = preferences.savedFeeds.get().filterNot { it.presetId == presetId }
            preferences.savedFeeds.set(remainingFeeds)
            removedFeedIds.forEach(::clearTimeline)

            val selectedFeedId = preferences.selectedFeedId.get()
            if (selectedFeedId.isNotBlank() && remainingFeeds.none { it.id == selectedFeedId }) {
                preferences.selectedFeedId.set(
                    remainingFeeds.firstOrNull {
                        it.enabled && it.contentMode == SourceFeedContentMode.Browse
                    }?.id.orEmpty(),
                )
            }
            val selectedVideoFeedId = preferences.selectedVideoFeedId.get()
            if (selectedVideoFeedId.isNotBlank() && remainingFeeds.none { it.id == selectedVideoFeedId }) {
                preferences.selectedVideoFeedId.set(
                    remainingFeeds.firstOrNull {
                        it.enabled && it.contentMode == SourceFeedContentMode.Video
                    }?.id.orEmpty(),
                )
            }
        }
    }

    fun createFeed(feed: SourceFeed) {
        preferences.savedFeeds.set(
            preferences.savedFeeds.get()
                .filterNot { it.id == feed.id }
                .plus(feed),
        )
        if (feed.enabled) {
            selectedFeedPreference(feed.contentMode).set(feed.id)
        }
    }

    fun updateFeed(feed: SourceFeed) {
        preferences.savedFeeds.set(
            preferences.savedFeeds.get().map { if (it.id == feed.id) feed else it },
        )
        if (feed.enabled) {
            selectedFeedPreference(feed.contentMode).set(feed.id)
        } else if (selectedFeedPreference(feed.contentMode).get() == feed.id) {
            selectedFeedPreference(feed.contentMode).set("")
        }
    }

    fun renameFeed(feedId: String, title: String?) {
        preferences.savedFeeds.set(
            preferences.savedFeeds.get().map { if (it.id == feedId) it.copy(title = title) else it },
        )
    }

    /** Removes the feed and returns what [restoreFeed] needs to put it back, or null if it did not exist. */
    fun removeFeed(feedId: String): RemovedFeed? {
        val feeds = preferences.savedFeeds.get()
        val index = feeds.indexOfFirst { it.id == feedId }
        if (index == -1) return null
        val timeline = preferences.feedTimeline(feedId)
        val anchor = preferences.feedAnchor(feedId)
        val selectedFeed = selectedFeedPreference(feeds[index].contentMode)
        val removed = RemovedFeed(
            feed = feeds[index],
            index = index,
            timeline = timeline.get().takeIf { timeline.isSet() },
            anchor = anchor.get().takeIf { anchor.isSet() },
            wasSelected = selectedFeed.get() == feedId,
        )

        preferences.savedFeeds.set(feeds.filterNot { it.id == feedId })
        clearTimeline(feedId)
        if (removed.wasSelected) {
            selectedFeed.set("")
        }
        return removed
    }

    fun restoreFeed(removed: RemovedFeed) {
        val feeds = preferences.savedFeeds.get()
        if (feeds.any { it.id == removed.feed.id }) return
        preferences.savedFeeds.set(
            feeds.toMutableList().apply { add(removed.index.coerceAtMost(size), removed.feed) },
        )
        removed.timeline?.let { saveTimeline(removed.feed.id, it) }
        removed.anchor?.let { saveAnchor(removed.feed.id, it) }
        if (removed.wasSelected) {
            selectedFeedPreference(removed.feed.contentMode).set(removed.feed.id)
        }
    }

    fun reorderFeed(fromIndex: Int, toIndex: Int) {
        val feeds = preferences.savedFeeds.get().toMutableList()
        if (fromIndex !in feeds.indices || toIndex !in feeds.indices || fromIndex == toIndex) {
            return
        }

        val movedFeed = feeds.removeAt(fromIndex)
        feeds.add(toIndex, movedFeed)
        preferences.savedFeeds.set(feeds)
    }

    fun selectFeed(feedId: String) {
        preferences.selectedFeedId.set(feedId)
    }

    fun selectVideoFeed(feedId: String) {
        preferences.selectedVideoFeedId.set(feedId)
    }

    fun timeline(feedId: String): Flow<SourceFeedTimeline> {
        return preferences.feedTimeline(feedId).changes()
    }

    fun timelineSnapshot(feedId: String): SourceFeedTimeline {
        return preferences.feedTimeline(feedId).get()
    }

    fun saveTimeline(feedId: String, timeline: SourceFeedTimeline) {
        preferences.feedTimeline(feedId).set(timeline)
    }

    fun clearTimeline(feedId: String) {
        preferences.feedTimeline(feedId).delete()
        preferences.feedAnchor(feedId).delete()
    }

    fun anchor(feedId: String): Flow<SourceFeedAnchor> {
        return preferences.feedAnchor(feedId).changes()
    }

    fun anchorSnapshot(feedId: String): SourceFeedAnchor {
        return preferences.feedAnchor(feedId).get()
    }

    fun saveAnchor(feedId: String, anchor: SourceFeedAnchor) {
        preferences.feedAnchor(feedId).set(anchor)
    }

    data class RemovedFeed(
        val feed: SourceFeed,
        val index: Int,
        val timeline: SourceFeedTimeline?,
        val anchor: SourceFeedAnchor?,
        val wasSelected: Boolean,
    )

    data class State(
        val presets: List<SourceFeedPreset>,
        val feeds: List<SourceFeed>,
        val selectedFeedId: String?,
        val selectedVideoFeedId: String? = null,
    )

    private fun selectedFeedPreference(contentMode: SourceFeedContentMode) = when (contentMode) {
        SourceFeedContentMode.Browse -> preferences.selectedFeedId
        SourceFeedContentMode.Video -> preferences.selectedVideoFeedId
    }
}

private fun SourceFeedPreset.feedBehaviorChanged(other: SourceFeedPreset): Boolean {
    return sourceId != other.sourceId ||
        listingMode != other.listingMode ||
        chronological != other.chronological ||
        query != other.query ||
        filters != other.filters
}
