package mihon.feature.profiles.ui.switcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.feature.profiles.ui.components.ProfileAvatar
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.AvatarSize
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** The Menu tab's card for the active profile, opening the switcher; absent with one profile. */
@Composable
internal fun ActiveProfileCard(modifier: Modifier = Modifier) {
    val switcher = LocalProfileSwitcher.current ?: return
    Card(modifier = modifier, onClick = switcher.open) {
        Row(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(colorSeed = switcher.activeProfile.colorSeed, size = AvatarSize.Row)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = switcher.activeProfile.name, role = TextRole.ItemTitle, maxLines = 1)
                Text(
                    text = pluralStringResource(
                        MR.plurals.profiles_count,
                        switcher.profileCount,
                        switcher.profileCount,
                    ),
                    role = TextRole.Supporting,
                )
            }
            Button(
                label = stringResource(MR.strings.action_switch),
                onClick = switcher.open,
                style = ButtonStyle.Tonal,
            )
        }
    }
}
