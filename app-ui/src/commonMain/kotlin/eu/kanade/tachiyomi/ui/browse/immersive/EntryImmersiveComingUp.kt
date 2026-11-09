package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.content.BrowseMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.entryCoverScale
import app.katari.presentation.kit.surface.MediaBackdrop
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.currentWindowLayout
import coil3.compose.AsyncImage
import eu.kanade.presentation.entry.components.EntryCover
import tachiyomi.domain.entry.model.Entry

/**
 * What a page of the feed shows until its media does, for every kind of title: the title's cover blurred across the
 * page, and above the middle, where the loading spinner turns, the cover itself with the title's name and the episode
 * or chapter shown, so a title coming up says what it is before it plays. A short window leaves the cover out.
 */
@Composable
internal fun EntryImmersiveComingUp(entry: Entry, chapterName: String?, modifier: Modifier = Modifier) {
    val spacing = KatariTheme.spacing
    Box(modifier = modifier) {
        MediaBackdrop(
            modifier = Modifier.fillMaxSize(),
            image = { imageModifier ->
                AsyncImage(
                    model = entry,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier,
                )
            },
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xl)
                    .padding(bottom = spacing.xxl * 2),
                verticalArrangement = Arrangement.spacedBy(spacing.s, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!currentWindowLayout().isShort) {
                    EntryCover.Book(
                        data = entry,
                        modifier = Modifier
                            .padding(bottom = spacing.m)
                            .width(BrowseMetrics.immersiveLoadingCoverWidth * entryCoverScale()),
                        raised = true,
                    )
                }
                Text(
                    text = entry.title,
                    role = TextRole.SectionTitle,
                    tone = ContentTone.OnMedia,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                chapterName?.let { name ->
                    Text(
                        text = name,
                        role = TextRole.Supporting,
                        tone = ContentTone.OnMediaVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
