package eu.kanade.presentation.more.settings.screen.libraryupdates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.feature.library.update.planning.LibraryUpdatePreview
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** When the next automatic library update runs, if any, and how much of the library it will check. */
@Composable
internal fun LibraryUpdateNextRunCard(nextRunAt: Long?, preview: LibraryUpdatePreview?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        container = SurfaceContainer.PrimaryContainer,
        shape = KatariTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        ) {
            Text(
                text = if (nextRunAt != null) {
                    "${stringResource(MR.strings.library_updates_next_automatic)} · " +
                        relativeTimeSpanString(nextRunAt)
                } else {
                    stringResource(MR.strings.library_updates_automatic_off)
                },
                role = TextRole.SectionTitle,
            )
            if (preview != null) {
                Text(
                    text = pluralStringResource(
                        if (nextRunAt != null) {
                            MR.plurals.library_updates_checks_of
                        } else {
                            MR.plurals.library_updates_manual_checks_of
                        },
                        preview.librarySize,
                        preview.checks,
                        preview.librarySize,
                    ),
                    role = TextRole.Body,
                )
            }
        }
    }
}
