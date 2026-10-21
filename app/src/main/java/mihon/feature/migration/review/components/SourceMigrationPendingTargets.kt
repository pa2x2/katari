package mihon.feature.migration.review.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import eu.kanade.presentation.entry.components.EntryCover
import mihon.feature.migration.session.model.SourceMigrationItemState
import mihon.presentation.kit.content.CoverBacking
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.coverFrame
import mihon.presentation.kit.content.skeleton
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/*
 * What stands in a discovery card where an entry's replacement goes before there is one: a pulsing outline of an entry
 * while the search runs or waits, and a prompt to choose one by hand once the search found nothing. Both keep the
 * cover slot of a found replacement, so the card does not jump when the result arrives.
 */

@Composable
internal fun SourceMigrationTargetPlaceholder(state: SourceMigrationItemState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sourceMigrationSkeleton")
    val pulse by transition.animateFloat(
        initialValue = SKELETON_PULSE_FROM,
        targetValue = SKELETON_PULSE_TO,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SKELETON_PULSE_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sourceMigrationSkeletonPulse",
    )
    val skeleton = Modifier.graphicsLayer { alpha = pulse }

    Card(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.High) {
        Row(
            modifier = Modifier.padding(KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBlock(skeleton.then(CoverSlot))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                SkeletonBlock(
                    skeleton
                        .fillMaxWidth(SKELETON_TITLE_FRACTION)
                        .height(KatariTheme.spacing.m),
                )
                SkeletonBlock(
                    skeleton
                        .fillMaxWidth(SKELETON_SUBTITLE_FRACTION)
                        .height(KatariTheme.spacing.m),
                )
            }
            SourceMigrationSearchStatusPill(state)
        }
    }
}

@Composable
internal fun SourceMigrationMissingTarget(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.High, onClick = onClick) {
        Row(
            modifier = Modifier.padding(KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = CoverSlot
                    .coverFrame(CoverFrame.Thumbnail, CoverBacking.Empty),
            )
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_chooseReplacement),
                    role = TextRole.ItemTitle,
                )
                Text(
                    text = stringResource(MR.strings.sourceMigrationReview_noAutomaticMatch),
                    role = TextRole.Supporting,
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier) {
    Box(modifier = modifier.skeleton())
}

/** The cover slot of [SourceMigrationEntryListItem], kept empty. */
private val CoverSlot: Modifier
    @Composable get() = Modifier
        .height(KatariTheme.sizes.artworkLarge)
        .aspectRatio(EntryCover.Book.ratio)

private const val SKELETON_PULSE_FROM = 0.4f
private const val SKELETON_PULSE_TO = 1f
private const val SKELETON_PULSE_MILLIS = 700
private const val SKELETON_TITLE_FRACTION = 0.8f
private const val SKELETON_SUBTITLE_FRACTION = 0.55f
