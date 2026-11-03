package eu.kanade.presentation.browse.components

import dev.icerock.moko.resources.StringResource
import mihon.domain.extension.model.ContentWarning
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*

internal data class ContentWarningLabel(
    val title: StringResource,
    val description: StringResource,
    val tone: ContentTone,
    /** Whether rows say it too, or only Extension info; every extension of a store that doesn't rate has it. */
    val onRows: Boolean = true,
)

internal val ContentWarning.label: ContentWarningLabel?
    get() = when (this) {
        ContentWarning.SAFE -> null
        ContentWarning.MIXED -> ContentWarningLabel(
            title = MR.strings.ext_content_warning_mixed,
            description = MR.strings.ext_content_warning_mixed_description,
            tone = ContentTone.Primary,
        )
        ContentWarning.NSFW -> ContentWarningLabel(
            title = MR.strings.ext_content_warning_nsfw,
            description = MR.strings.ext_content_warning_nsfw_description,
            tone = ContentTone.Error,
        )
        ContentWarning.NOT_RATED -> ContentWarningLabel(
            title = MR.strings.ext_content_warning_not_rated,
            description = MR.strings.ext_content_warning_not_rated_description,
            tone = ContentTone.Secondary,
            onRows = false,
        )
    }
