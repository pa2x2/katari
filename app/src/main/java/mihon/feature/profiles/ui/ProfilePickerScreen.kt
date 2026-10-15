package mihon.feature.profiles.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowInsetsControllerCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.ui.components.ProfilePickerTile
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class ProfilePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val profileManager = remember { context.appGraph.profileManager }
        val uiPreferences = remember { context.appGraph.uiPreferences }

        val profiles by profileManager.visibleProfiles.collectAsState()
        val activeProfile by profileManager.activeProfile.collectAsState()

        ProfilePickerScene(
            profiles = profiles,
            activeProfileId = activeProfile?.id,
            onProfileSelected = { profile ->
                scope.launch {
                    val switched = switchToProfile(
                        context = context,
                        profileManager = profileManager,
                        uiPreferences = uiPreferences,
                        profile = profile,
                    )
                    if (switched) {
                        navigator.popUntilRoot()
                    }
                }
            },
            onOpenManagement = { navigator.push(ProfilesSettingsScreen()) },
        )
    }
}

@Composable
fun ProfilePickerScene(
    profiles: List<Profile>,
    activeProfileId: Long?,
    onProfileSelected: (Profile) -> Unit,
    onOpenManagement: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val view = LocalView.current
    val spacing = KatariTheme.spacing
    val compactLayout = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val contentVerticalPadding = if (compactLayout) spacing.m else spacing.l
    val titleTopPadding = when {
        !compactLayout -> spacing.xxl
        onOpenManagement == null -> KatariTheme.sizes.none
        else -> spacing.m
    }
    val looseSpacing = if (compactLayout) spacing.l else spacing.xl

    DisposableEffect(context, view) {
        val activity = context as? Activity
        if (activity == null) {
            onDispose {}
        } else {
            val controller = WindowInsetsControllerCompat(activity.window, view)
            val previousStatusBars = controller.isAppearanceLightStatusBars
            val previousNavigationBars = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false

            onDispose {
                controller.isAppearanceLightStatusBars = previousStatusBars
                controller.isAppearanceLightNavigationBars = previousNavigationBars
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), container = SurfaceContainer.Surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl, vertical = contentVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compactLayout || onOpenManagement != null) {
                ProfilePickerHeader(onOpenManagement = onOpenManagement)
            }
            if (!compactLayout) {
                Icon(
                    painter = painterResource(R.drawable.ic_katari),
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
                        onClick = { onProfileSelected(profile) },
                    )
                }
            }
        }
    }
}

/** The row above the picker: the way into profile management when there is one, or the room it would take. */
@Composable
private fun ProfilePickerHeader(onOpenManagement: (() -> Unit)?) {
    if (onOpenManagement == null) {
        Spacer(modifier = Modifier.height(KatariTheme.sizes.touchTarget))
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(
            icon = MaterialSymbols.Rounded.Settings,
            contentDescription = stringResource(MR.strings.profiles_manage_title),
            onClick = onOpenManagement,
        )
    }
}
