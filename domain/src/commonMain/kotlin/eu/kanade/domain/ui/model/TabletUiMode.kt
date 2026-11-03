package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/** Whether wide windows get wide layouts. Values saved as the removed ALWAYS and LANDSCAPE read back as [AUTOMATIC]. */
enum class TabletUiMode(val titleRes: StringResource) {
    AUTOMATIC(MR.strings.automatic_background),
    NEVER(MR.strings.lock_never),
}
