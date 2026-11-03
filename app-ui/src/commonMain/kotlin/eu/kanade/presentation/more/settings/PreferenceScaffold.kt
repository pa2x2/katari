package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    isProfileSpecific: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(titleRes),
                        titleSuffix = if (isProfileSpecific) {
                            { ProfileSpecificChip() }
                        } else {
                            null
                        },
                    )
                },
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = it,
            )
        },
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
            )
        },
    )
}
