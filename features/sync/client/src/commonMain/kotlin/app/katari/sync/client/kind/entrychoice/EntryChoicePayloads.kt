package app.katari.sync.client.kind.entrychoice

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Whether library updates check the entry regardless of its category's rules: `always` or `never`. */
@Serializable
data class EntryUpdateModePayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val mode: Stamped<String>,
)

/** A scanlator whose chapters the entry hides. */
@Serializable
data class ExcludedScanlatorPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val scanlator: String,
    @ProtoNumber(3) val removed: Stamped<Boolean>,
)

/** One reader or player setting the entry overrides, encoded by the viewer that owns it. */
@Serializable
data class ViewerSettingPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val providerId: String,
    @ProtoNumber(3) val settingKey: String,
    @ProtoNumber(4) val encodedValue: Stamped<String>,
)

@Serializable
data class DownloadPreferencesPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val dubKey: Stamped<String?>,
    @ProtoNumber(3) val streamKey: Stamped<String?>,
    @ProtoNumber(4) val subtitleKey: Stamped<String?>,
    @ProtoNumber(5) val qualityMode: Stamped<String>,
    @ProtoNumber(6) val serverKey: Stamped<String?>,
    @ProtoNumber(7) val audioKey: Stamped<String?>,
)

@Serializable
data class TranslationLanguagesPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val contentLanguage: Stamped<String?>,
    @ProtoNumber(3) val targetLanguage: Stamped<String?>,
    @ProtoNumber(4) val translateDownloads: Stamped<Boolean>,
)

/** The AniList or MyAnimeList series the person picked for a season group's intro and outro times. */
@Serializable
data class IntroTimesPickPayload(
    @ProtoNumber(1) val entryKey: String,
    /** The season group, empty for the whole entry. */
    @ProtoNumber(2) val groupKey: String,
    @ProtoNumber(3) val match: Stamped<IntroTimesMatch>,
)

@Serializable
data class IntroTimesMatch(
    @ProtoNumber(1) val anilistId: Long? = null,
    @ProtoNumber(2) val malId: Long? = null,
    @ProtoNumber(3) val title: String? = null,
    @ProtoNumber(4) val year: Long? = null,
)

/** The series or film the person picked for a season group's TheIntroDB times. */
@Serializable
data class TheIntroDbPickPayload(
    @ProtoNumber(1) val entryKey: String,
    /** The season group, empty for the whole entry. */
    @ProtoNumber(2) val groupKey: String,
    @ProtoNumber(3) val match: Stamped<TheIntroDbPick>,
)

/** Without [title] it is None; [season] and [firstEpisode] are null for a film. */
@Serializable
data class TheIntroDbPick(
    @ProtoNumber(1) val tvdbId: Long? = null,
    @ProtoNumber(2) val tmdbId: Long? = null,
    @ProtoNumber(3) val imdbId: String? = null,
    @ProtoNumber(4) val title: String? = null,
    @ProtoNumber(5) val year: Long? = null,
    @ProtoNumber(6) val season: Long? = null,
    @ProtoNumber(7) val firstEpisode: Long? = null,
)

/** What the player picks for the entry. Picture framing, zoom and pan stay with each screen. */
@Serializable
data class PlaybackPreferencesPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val dubKey: Stamped<String?>,
    @ProtoNumber(3) val streamKey: Stamped<String?>,
    @ProtoNumber(4) val sourceQualityKey: Stamped<String?>,
    @ProtoNumber(5) val subtitleKey: Stamped<String?>,
    @ProtoNumber(6) val playerQualityMode: Stamped<String>,
    @ProtoNumber(7) val playerQualityHeight: Stamped<Long?>,
    @ProtoNumber(8) val subtitleOffsetX: Stamped<Double?>,
    @ProtoNumber(9) val subtitleOffsetY: Stamped<Double?>,
    @ProtoNumber(10) val subtitleTextSize: Stamped<Double?>,
    /** ARGB. */
    @ProtoNumber(11) val subtitleTextColor: Stamped<Long?>,
    @ProtoNumber(12) val subtitleBackgroundColor: Stamped<Long?>,
    @ProtoNumber(13) val subtitleBackgroundOpacity: Stamped<Double?>,
    @ProtoNumber(14) val subtitleTextEdge: Stamped<String?>,
    @ProtoNumber(15) val playerEngine: Stamped<String?>,
    @ProtoNumber(16) val serverKey: Stamped<String?>,
    @ProtoNumber(17) val subtitlePosition: Stamped<String?> = Stamped(null, Stamped.UNKNOWN_EDIT),
)
