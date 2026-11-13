package app.katari.sync.client.kind.subtitletiming

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** How late an episode's subtitles show, as the person set it while watching it. */
@Serializable
data class SubtitleTimingPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val chapterKey: String,
    @ProtoNumber(3) val timing: Stamped<SubtitleDelay>,
)

/** The subtitle the timing was set for, and [delayMs] milliseconds later, or earlier when negative; never 0. */
@Serializable
data class SubtitleDelay(
    @ProtoNumber(1) val subtitleKey: String,
    @ProtoNumber(2) val delayMs: Long,
)
