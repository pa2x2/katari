package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Info
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.OverlayPanel
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The title and its actions in the room beside tall media, always shown so they never cover the picture: the view's
 * own actions along the top, then the title and episode or chapter over labelled buttons, opening it first.
 */
@Composable
internal fun ImmersiveSidePanel(
    view: ImmersiveViewActions,
    entry: ImmersiveEntryActions,
    modifier: Modifier = Modifier,
) {
    OverlayPanel(modifier = modifier.fillMaxSize().mediaControl(), style = OverlayStyle.Scrim) {
        Column(
            modifier = Modifier.fillMaxSize().padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
                ImmersiveViewActionButtons(view)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = entry.entry.displayTitle,
                role = TextRole.SectionTitle,
                tone = ContentTone.OnMedia,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            entry.chapterName?.let {
                Text(
                    text = it,
                    role = TextRole.Supporting,
                    tone = ContentTone.OnMediaVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val presentation = entry.entry.type.entryTypePresentation()
            Button(
                label = stringResource(presentation.immersiveOpenLabel),
                onClick = { entry.onOpenChapter?.invoke() },
                modifier = Modifier.fillMaxWidth().mediaControl(),
                icon = presentation.immersiveOpenIcon,
                enabled = entry.onOpenChapter != null,
            )
            Button(
                label = libraryActionLabel(entry.entry),
                onClick = entry.onLibraryAction,
                modifier = Modifier.fillMaxWidth().mediaControl(),
                style = ButtonStyle.Tonal,
                icon = EntryActionIcons.library(entry.entry.favorite),
            )
            Button(
                label = stringResource(MR.strings.action_details),
                onClick = entry.onDetails,
                modifier = Modifier.fillMaxWidth().mediaControl(),
                style = ButtonStyle.Tonal,
                icon = MaterialSymbols.Rounded.Info,
            )
        }
    }
}
