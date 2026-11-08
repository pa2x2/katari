package eu.kanade.tachiyomi.ui.video.player.introtimes

import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.anime.introtimes.IntroTimesService
import mihon.presentation.kit.control.MediaMarkSource
import tachiyomi.i18n.*

internal val IntroTimesService.label: StringResource
    get() = when (this) {
        IntroTimesService.ANISKIP -> MR.strings.intro_times_service_aniskip
        IntroTimesService.THEINTRODB -> MR.strings.intro_times_service_theintrodb
    }

/** The colour the service's spans take on the timeline and beside its name. */
internal val IntroTimesService.markSource: MediaMarkSource
    get() = when (this) {
        IntroTimesService.ANISKIP -> MediaMarkSource.First
        IntroTimesService.THEINTRODB -> MediaMarkSource.Second
    }
