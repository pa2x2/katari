package eu.kanade.presentation.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.TonedSpan
import app.katari.presentation.kit.content.TonedText
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import eu.kanade.presentation.library.update.libraryUpdateTimeOn
import eu.kanade.presentation.util.relativeTimeSpanString
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

internal fun LazyListScope.updatesLastUpdatedItem(
    lastUpdated: Long,
    failed: Int,
    otherDevice: OtherDeviceLibraryUpdate?,
    pausedSources: List<String>,
    onClick: (() -> Unit)?,
) {
    item(key = "updates-lastUpdated") {
        UpdatesLastUpdatedHeader(
            lastUpdated = lastUpdated,
            failed = failed,
            otherDevice = otherDevice,
            pausedSources = pausedSources,
            onClick = onClick,
            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
        )
    }
}

/**
 * @param lastUpdated when this device last updated the library.
 * @param failed entries the latest update failed to check, named so a failure is noticed without opening the report.
 * @param otherDevice another device's update of the whole library that started after [lastUpdated], named in place of
 * [lastUpdated] and [failed].
 * @param pausedSources the sources library updates leave out, named so a pause isn't forgotten.
 * @param onClick opens the latest update's report; null while there is none.
 */
@Composable
internal fun UpdatesLastUpdatedHeader(
    lastUpdated: Long,
    failed: Int,
    otherDevice: OtherDeviceLibraryUpdate?,
    pausedSources: List<String>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val updatedAt = if (otherDevice != null) {
        libraryUpdateTimeOn(relativeTimeSpanString(otherDevice.summary.run.startedAt), otherDevice.deviceName)
    } else {
        relativeTimeSpanString(lastUpdated)
    }
    val latestFailed = otherDevice?.summary?.failed ?: failed
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
    ) {
        TonedText(
            spans = listOfNotNull(
                TonedSpan(stringResource(MR.strings.updates_last_update_info, updatedAt)),
                TonedSpan(
                    text = pluralStringResource(MR.plurals.library_updates_failed, latestFailed, latestFailed),
                    tone = ContentTone.Error,
                    keepWhole = true,
                ).takeIf { latestFailed > 0 },
            ),
            role = TextRole.Supporting,
            separator = " · ",
        )
        if (pausedSources.isNotEmpty()) {
            Text(
                text = pausedSourcesText(pausedSources),
                role = TextRole.Supporting,
            )
        }
    }
}

@Composable
private fun pausedSourcesText(names: List<String>): String {
    return when (names.size) {
        1 -> stringResource(MR.strings.updates_paused_source, names[0])
        2 -> stringResource(MR.strings.updates_paused_two_sources, names[0], names[1])
        else -> pluralStringResource(MR.plurals.updates_paused_sources, names.size, names.size)
    }
}
