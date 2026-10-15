package eu.kanade.presentation.more.settings.widget

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import mihon.presentation.kit.list.ThemeChoiceRow

@Composable
internal fun AppThemePreferenceWidget(
    value: AppTheme,
    amoled: Boolean,
    onItemClick: (AppTheme) -> Unit,
) {
    val context = LocalContext.current
    val appThemes = remember {
        AppTheme.entries
            .filterNot { it.titleRes == null || (it == AppTheme.MONET && !DeviceUtil.isDynamicColorAvailable) }
    }
    ThemeChoiceRow(
        themes = appThemes,
        selected = value,
        amoled = amoled,
        onSelect = { appTheme ->
            onItemClick(appTheme)
            (context as? Activity)?.let { ActivityCompat.recreate(it) }
        },
    )
}
