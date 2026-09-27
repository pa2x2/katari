package eu.kanade.presentation.more.settings.screen.translation.series

import androidx.compose.runtime.Composable
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.NavigationItem
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Opens the series languages list from Translation settings, saying how many series use their own. */
@Composable
internal fun SeriesTranslationLanguagesEntry(
    count: Int?,
    onClick: () -> Unit,
) {
    NavigationItem(
        label = stringResource(MR.strings.translation_series_languages),
        subtitle = when (count) {
            null -> null
            0 -> stringResource(MR.strings.translation_series_languages_none)
            else -> pluralStringResource(MR.plurals.translation_series_languages_count, count, count)
        },
        onClick = onClick,
    )
}
