package eu.kanade.domain.source.service

import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedAnchor
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.SourceFeedTimeline
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mihon.domain.migration.models.MigrationFlag
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.preference.getLongArray
import tachiyomi.domain.library.model.LibraryDisplayMode

class SourcePreferences(
    private val preferenceStore: PreferenceStore,
    private val json: Json,
) {
    companion object {
        const val LEGACY_HIDDEN_SOURCES_KEY = "hidden_catalogues"
        const val HIDDEN_SOURCES_KEY = "hidden_sources"
        const val MANGA_HIDDEN_SOURCES_KEY = "hidden_manga_catalogues"
        const val ANIME_HIDDEN_SOURCES_KEY = "hidden_anime_catalogues"
        const val PINNED_SOURCES_KEY = "pinned_sources"
        const val MANGA_PINNED_SOURCES_KEY = "pinned_catalogues"
        const val ANIME_PINNED_SOURCES_KEY = "pinned_anime_catalogues"
        const val LAST_USED_SOURCE_KEY = "last_source"
        const val MANGA_LAST_USED_SOURCE_KEY = "last_catalogue_source"
        const val ANIME_LAST_USED_SOURCE_KEY = "last_anime_catalogue_source"

        val SOURCE_DISPLAY_MODE_KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix("pref_display_mode_catalogue_")
        val FEED_TIMELINE_KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix(
            Preference.appStateKey("source_feed_timeline_"),
        )
        val FEED_ANCHOR_KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix(
            Preference.appStateKey("source_feed_anchor_"),
        )

        val profileKeyPatterns = setOf(
            SOURCE_DISPLAY_MODE_KEY_FAMILY,
            FEED_TIMELINE_KEY_FAMILY,
            FEED_ANCHOR_KEY_FAMILY,
        )
    }

    val sourceDisplayMode: Preference<LibraryDisplayMode> = preferenceStore.getObjectFromString(
        "pref_display_mode_catalogue",
        LibraryDisplayMode.default,
        LibraryDisplayMode.Serializer::serialize,
        LibraryDisplayMode.Serializer::deserialize,
    )

    fun sourceDisplayMode(sourceId: Long): Preference<LibraryDisplayMode> {
        return preferenceStore.getObjectFromString(
            SOURCE_DISPLAY_MODE_KEY_FAMILY.key(sourceId),
            sourceDisplayMode.get(),
            LibraryDisplayMode.Serializer::serialize,
            LibraryDisplayMode.Serializer::deserialize,
        )
    }

    val enabledLanguages: Preference<Set<String>> = preferenceStore.getStringSet(
        "source_languages",
        LocaleHelper.getDefaultEnabledLanguages(),
    )

    // Keep the original key so selections made before the filter was shared with Sources survive.
    val browseContentTypeFilter: Preference<Set<String>> = preferenceStore.getStringSet(
        Preference.appStateKey("extension_content_type_filter"),
        emptySet(),
    )

    val disabledSources: Preference<Set<String>> = preferenceStore.getStringSet(HIDDEN_SOURCES_KEY, emptySet())

    val incognitoExtensions: Preference<Set<String>> = preferenceStore.getStringSet("incognito_extensions", emptySet())

    val pinnedSources: Preference<Set<String>> = preferenceStore.getStringSet(PINNED_SOURCES_KEY, emptySet())

    val lastUsedSource: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey(LAST_USED_SOURCE_KEY),
        -1,
    )

    val showNsfwSource: Preference<Boolean> = preferenceStore.getBoolean("show_nsfw_source", true)

    val migrationSortingMode: Preference<SetMigrateSorting.Mode> = preferenceStore.getEnum(
        "pref_migration_sorting",
        SetMigrateSorting.Mode.ALPHABETICAL,
    )

    val migrationSortingDirection: Preference<SetMigrateSorting.Direction> = preferenceStore.getEnum(
        "pref_migration_direction",
        SetMigrateSorting.Direction.ASCENDING,
    )

    val hideInLibraryItems: Preference<Boolean> = preferenceStore.getBoolean("browse_hide_in_library_items", false)

    val globalSearchFilterState: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("has_filters_toggle_state"),
        false,
    )

    val migrationSources: Preference<List<Long>> = preferenceStore.getLongArray("migration_sources", emptyList())

    val migrationFlags: Preference<Set<MigrationFlag>> = preferenceStore.getObjectFromInt(
        key = "migration_flags",
        defaultValue = MigrationFlag.entries.toSet(),
        serializer = { MigrationFlag.toBit(it) },
        deserializer = { value: Int -> MigrationFlag.fromBit(value) },
    )

    val migrationDeepSearchMode: Preference<Boolean> = preferenceStore.getBoolean("migration_deep_search", false)

    val migrationPrioritizeByChapters: Preference<Boolean> = preferenceStore.getBoolean(
        "migration_prioritize_by_chapters",
        false,
    )

    val migrationHideUnmatched: Preference<Boolean> = preferenceStore.getBoolean("migration_hide_unmatched", false)

    val migrationHideWithoutUpdates: Preference<Boolean> = preferenceStore.getBoolean(
        "migration_hide_without_updates",
        false,
    )

    val savedFeedPresets: Preference<List<SourceFeedPreset>> = preferenceStore.getObjectFromString(
        key = "saved_feed_presets",
        defaultValue = emptyList(),
        serializer = { json.encodeToString(it) },
        deserializer = { json.decodeFromString<List<SourceFeedPreset>>(it) },
    )

    val savedFeeds: Preference<List<SourceFeed>> = preferenceStore.getObjectFromString(
        key = "saved_source_feeds",
        defaultValue = emptyList(),
        serializer = { json.encodeToString(it) },
        deserializer = { json.decodeFromString<List<SourceFeed>>(it) },
    )

    val selectedFeedId: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("selected_source_feed_id"),
        "",
    )

    val selectedVideoFeedId: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("selected_video_source_feed_id"),
        "",
    )

    fun feedTimeline(feedId: String): Preference<SourceFeedTimeline> {
        return preferenceStore.getObjectFromString(
            key = FEED_TIMELINE_KEY_FAMILY.key(feedId),
            defaultValue = SourceFeedTimeline(),
            serializer = { json.encodeToString(it) },
            deserializer = { json.decodeFromString<SourceFeedTimeline>(it) },
        )
    }

    fun feedAnchor(feedId: String): Preference<SourceFeedAnchor> {
        return preferenceStore.getObjectFromString(
            key = FEED_ANCHOR_KEY_FAMILY.key(feedId),
            defaultValue = SourceFeedAnchor(),
            serializer = { json.encodeToString(it) },
            deserializer = { json.decodeFromString<SourceFeedAnchor>(it) },
        )
    }
}

class ProfileSourcePreferences(
    private val profileStore: ProfileStore,
    private val json: Json,
) {
    fun forProfile(profileId: Long): SourcePreferences {
        return SourcePreferences(
            preferenceStore = profileStore.profileStore(profileId),
            json = json,
        )
    }
}
