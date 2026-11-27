package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import app.katari.core.common.extensions.JsonObjectEmptyBytes
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonCategory
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonExtensionSource
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonFeed
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonSavedSearch
import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonTracking
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/*
 * The Aniyomi family's backup, as far as converting it needs: field numbers from Aniyomi's
 * app/src/main/java/eu/kanade/tachiyomi/data/backup/models (aniyomiorg/aniyomi 97414446b8a9). Aniyomi keeps Mihon's
 * manga, categories, sources, extension stores and tracker links, which decode into the Mihon format's models. Fields
 * not declared here are skipped.
 */

/** The backup without its entries, as either layout holds it. */
internal class AniyomiBackup(
    val mangaCategories: List<MihonCategory>,
    val animeCategories: List<MihonCategory>,
    val mangaSources: List<BackupSource>,
    val animeSources: List<BackupSource>,
    val preferences: List<BackupPreference>,
    /** Manga and anime sources' settings in one list. */
    val sourcePreferences: List<BackupSourcePreferences>,
    val extensionSources: List<MihonExtensionSource>,
    /** Anikku's, as Komikku keeps them. */
    val savedSearches: List<MihonSavedSearch>,
    val feeds: List<MihonFeed>,
    val customButtons: List<AniyomiCustomButton>,
)

/** Aniyomi 0.18.1 and later: anime from field 501 on, marked by `isLegacy` at 500, which these always write as false. */
@Serializable
internal class AniyomiCurrentBackup(
    @ProtoNumber(2) val mangaCategories: List<MihonCategory> = emptyList(),
    @ProtoNumber(101) val mangaSources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) val preferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) val sourcePreferences: List<BackupSourcePreferences> = emptyList(),
    /** Aniyomi writes a repo, Animetail a store. */
    @ProtoNumber(106) val mangaExtensionSources: List<MihonExtensionSource> = emptyList(),
    @ProtoNumber(502) val animeCategories: List<MihonCategory> = emptyList(),
    @ProtoNumber(503) val animeSources: List<BackupSource> = emptyList(),
    /** Aniyomi 0.18.2 and later write a store, 0.18.1 and Anikku a repo. */
    @ProtoNumber(505) val animeExtensionSources: List<MihonExtensionSource> = emptyList(),
    @ProtoNumber(506) val customButtons: List<AniyomiCustomButton> = emptyList(),
    @ProtoNumber(600) val savedSearches: List<MihonSavedSearch> = emptyList(),
    @ProtoNumber(610) val feeds: List<MihonFeed> = emptyList(),
) {
    fun toAniyomiBackup() = AniyomiBackup(
        mangaCategories = mangaCategories,
        animeCategories = animeCategories,
        mangaSources = mangaSources,
        animeSources = animeSources,
        preferences = preferences,
        sourcePreferences = sourcePreferences,
        extensionSources = mangaExtensionSources + animeExtensionSources,
        savedSearches = savedSearches,
        feeds = feeds,
        customButtons = customButtons,
    )
}

/** Aniyomi 0.18.0 and earlier: anime at 3, its categories at 4 and its sources at 103. */
@Serializable
internal class AniyomiLegacyBackup(
    @ProtoNumber(2) val mangaCategories: List<MihonCategory> = emptyList(),
    @ProtoNumber(4) val animeCategories: List<MihonCategory> = emptyList(),
    @ProtoNumber(101) val mangaSources: List<BackupSource> = emptyList(),
    @ProtoNumber(103) val animeSources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) val preferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) val sourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(107) val animeExtensionSources: List<MihonExtensionSource> = emptyList(),
    @ProtoNumber(108) val mangaExtensionSources: List<MihonExtensionSource> = emptyList(),
    @ProtoNumber(109) val customButtons: List<AniyomiCustomButton> = emptyList(),
    @ProtoNumber(600) val savedSearches: List<MihonSavedSearch> = emptyList(),
    @ProtoNumber(610) val feeds: List<MihonFeed> = emptyList(),
) {
    fun toAniyomiBackup() = AniyomiBackup(
        mangaCategories = mangaCategories,
        animeCategories = animeCategories,
        mangaSources = mangaSources,
        animeSources = animeSources,
        preferences = preferences,
        sourcePreferences = sourcePreferences,
        extensionSources = mangaExtensionSources + animeExtensionSources,
        savedSearches = savedSearches,
        feeds = feeds,
        customButtons = customButtons,
    )
}

/** A button Aniyomi's video player shows, running the mpv Lua script the person wrote. */
@Serializable
internal class AniyomiCustomButton(@ProtoNumber(1) val name: String = "")

/** An entry's source alone, read to learn every source key before any entry is written. */
@Serializable
internal class AniyomiEntrySource(@ProtoNumber(1) val source: Long)

@Serializable
internal class AniyomiAnime(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(4) val artist: String? = null,
    @ProtoNumber(5) val author: String? = null,
    @ProtoNumber(6) val description: String? = null,
    @ProtoNumber(7) val genre: List<String> = emptyList(),
    /** Katari's `EntryStatus` codes, 7 (upcoming) included. */
    @ProtoNumber(8) val status: Int = 0,
    @ProtoNumber(9) val thumbnailUrl: String? = null,
    @ProtoNumber(13) val dateAdded: Long = 0,
    @ProtoNumber(16) val episodes: List<AniyomiEpisode> = emptyList(),
    /** The [MihonCategory.order] of each of the anime's categories in the anime category list. */
    @ProtoNumber(17) val categories: List<Long> = emptyList(),
    @ProtoNumber(18) val tracking: List<MihonTracking> = emptyList(),
    @ProtoNumber(100) val favorite: Boolean = true,
    /** The episode list's sort, filters and display, as `Anime.EPISODE_*` lays them out. */
    @ProtoNumber(101) val episodeFlags: Long = 0,
    /** The skip-intro length in its lowest byte, then the next airing episode and time trackers last gave. */
    @ProtoNumber(103) val viewerFlags: Long = 0,
    /** Keyed by episode URL. */
    @ProtoNumber(104) val history: List<BackupHistory> = emptyList(),
    @ProtoNumber(105) val updateStrategy: EntryUpdateStrategy = EntryUpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(107) val favoriteModifiedAt: Long? = null,
    /** Anikku's. */
    @ProtoNumber(108) val excludedScanlators: List<String> = emptyList(),
    /** Animetail's, Anikku's and Animiru's. */
    @ProtoNumber(110) val notes: String = "",
    /** Anikku's and Animiru's. */
    @ProtoNumber(111) val initialized: Boolean = false,
    /** In Aniyomi 0.18.2 and later. */
    @ProtoNumber(112) val memo: ByteArray = JsonObjectEmptyBytes,
    /** Animiru's, epoch millis. */
    @ProtoNumber(113) val nextUpdate: Long = 0,
    // Animiru keeps its custom info and excluded scanlators at 200-206, Animetail and Anikku theirs at 602-603 and 800-805
    @ProtoNumber(200) val animiruCustomStatus: Int? = null,
    @ProtoNumber(201) val animiruCustomTitle: String? = null,
    @ProtoNumber(202) val animiruCustomArtist: String? = null,
    @ProtoNumber(203) val animiruCustomAuthor: String? = null,
    @ProtoNumber(204) val animiruCustomDescription: String? = null,
    @ProtoNumber(205) val animiruCustomGenre: List<String>? = null,
    @ProtoNumber(206) val animiruExcludedScanlators: List<String> = emptyList(),
    @ProtoNumber(500) val backgroundUrl: String? = null,
    @ProtoNumber(602) val customStatus: Int? = null,
    /** Anikku's. */
    @ProtoNumber(603) val customThumbnailUrl: String? = null,
    @ProtoNumber(800) val customTitle: String? = null,
    @ProtoNumber(801) val customArtist: String? = null,
    @ProtoNumber(802) val customAuthor: String? = null,
    @ProtoNumber(804) val customDescription: String? = null,
    @ProtoNumber(805) val customGenre: List<String>? = null,
    /** Animetail's audio language for the show: empty for the app's default, "original", or a language tag. */
    @ProtoNumber(1000) val audioLanguage: String = "",
)

@Serializable
internal class AniyomiEpisode(
    @ProtoNumber(1) val url: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val scanlator: String? = null,
    @ProtoNumber(4) val seen: Boolean = false,
    @ProtoNumber(5) val bookmark: Boolean = false,
    /** Where watching stopped, in milliseconds despite the name. */
    @ProtoNumber(6) val lastSecondSeen: Long = 0,
    @ProtoNumber(7) val dateFetch: Long = 0,
    @ProtoNumber(8) val dateUpload: Long = 0,
    @ProtoNumber(9) val episodeNumber: Float = 0F,
    @ProtoNumber(10) val sourceOrder: Long = 0,
    @ProtoNumber(13) val memo: ByteArray = JsonObjectEmptyBytes,
    /** Anikku's filler mark from before Aniyomi had one at 501. */
    @ProtoNumber(15) val legacyFillermark: Boolean = false,
    /** The episode's length, in milliseconds despite the name. */
    @ProtoNumber(16) val totalSeconds: Long = 0,
    /** In Aniyomi 0.18.1 and later. */
    @ProtoNumber(501) val fillermark: Boolean = false,
    @ProtoNumber(502) val summary: String? = null,
    @ProtoNumber(503) val previewUrl: String? = null,
    /** Animetail's upload date the person set by hand, shown in place of [dateUpload]; 0 when none is set. */
    @ProtoNumber(504) val dateUploadOverride: Long = 0,
)
