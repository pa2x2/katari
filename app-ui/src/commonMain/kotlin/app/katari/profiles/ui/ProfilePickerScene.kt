package app.katari.profiles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.presentation.kit.window.currentWindowLayout
import app.katari.profiles.core.Profile
import app.katari.profiles.ui.components.ProfilePickerTile
import app.katari.ui.resources.Res
import app.katari.ui.resources.ic_katari
import org.jetbrains.compose.resources.painterResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The full-screen profile picker shown on launch when the app is set to ask which profile to open. */
@Composable
fun ProfilePickerScene(
    profiles: List<Profile>,
    activeProfileId: Long?,
    isLocked: (Profile) -> Boolean,
    onProfileSelected: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    val compactLayout = currentWindowLayout().isShort
    val contentVerticalPadding = if (compactLayout) spacing.m else spacing.l
    val titleTopPadding = if (compactLayout) KatariTheme.sizes.none else spacing.xxl
    val looseSpacing = if (compactLayout) spacing.l else spacing.xl

    LightSystemBarIconsEffect()

    Surface(modifier = modifier.fillMaxSize(), container = SurfaceContainer.Surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl, vertical = contentVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compactLayout) {
                Spacer(modifier = Modifier.height(KatariTheme.sizes.touchTarget))
                Icon(
                    painter = painterResource(Res.drawable.ic_katari),
                    contentDescription = stringResource(MR.strings.app_name),
                    modifier = Modifier.size(KatariTheme.sizes.artwork),
                )
            }
            Text(
                text = stringResource(MR.strings.profiles_choose_profile),
                modifier = Modifier.padding(top = titleTopPadding, bottom = looseSpacing),
                role = if (compactLayout) TextRole.ScreenTitle else TextRole.SheetTitle,
                textAlign = TextAlign.Center,
            )

            FlowRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(looseSpacing, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(looseSpacing),
                maxItemsInEachRow = if (compactLayout) 4 else 3,
            ) {
                profiles.forEach { profile ->
                    ProfilePickerTile(
                        profile = profile,
                        isActive = profile.id == activeProfileId,
                        isLocked = isLocked(profile),
                        onClick = { onProfileSelected(profile) },
                    )
                }
            }
        }
    }
}
