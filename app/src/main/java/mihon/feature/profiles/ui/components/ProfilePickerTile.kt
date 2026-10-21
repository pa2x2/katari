package mihon.feature.profiles.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import mihon.feature.profiles.core.Profile
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Lock
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.AvatarTile
import mihon.presentation.kit.surface.AvatarTileSize
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A profile in the picker: its avatar with a face, a lock badge when it asks for authentication, and its name. */
@Composable
internal fun ProfilePickerTile(
    profile: Profile,
    isActive: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
) {
    val lockedLabel = stringResource(MR.strings.profiles_locked)
    val activeLabel = stringResource(MR.strings.profiles_active)
    val semanticsLabel = remember(profile.name, isActive, isLocked, activeLabel, lockedLabel) {
        buildString {
            append(profile.name)
            if (isActive) {
                append(", ")
                append(activeLabel)
            }
            if (isLocked) {
                append(", ")
                append(lockedLabel)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        modifier = Modifier.width(AvatarTileSize),
    ) {
        AvatarTile(
            seed = profile.colorSeed,
            selected = isActive,
            onClick = onClick,
            modifier = Modifier.semantics(mergeDescendants = true) {
                selected = isActive
                contentDescription = semanticsLabel
            },
        ) {
            ProfileFace(modifier = Modifier.fillMaxSize().padding(KatariTheme.spacing.l))
            if (isLocked) {
                OverlayPill(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KatariTheme.spacing.s),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(KatariTheme.spacing.xs)
                            .size(KatariTheme.sizes.iconSmall),
                    )
                }
            }
        }

        Text(
            text = profile.name,
            role = TextRole.Body,
            tone = if (isActive) ContentTone.Inherit else ContentTone.Secondary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
