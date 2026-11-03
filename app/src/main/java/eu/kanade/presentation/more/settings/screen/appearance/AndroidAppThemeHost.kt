package eu.kanade.presentation.more.settings.screen.appearance

import android.app.Activity
import androidx.core.app.ActivityCompat
import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import mihon.core.platform.PlatformContext

/** AppCompat keeps the night mode, and an activity reads its theme when it is created, so it is recreated. */
@Inject
class AndroidAppThemeHost : AppThemeHost {
    override val isDynamicColorAvailable: Boolean
        get() = DeviceUtil.isDynamicColorAvailable

    override fun applyThemeMode(mode: ThemeMode) = setAppCompatDelegateThemeMode(mode)

    override fun rebuildForTheme(context: PlatformContext) {
        (context as? Activity)?.let { ActivityCompat.recreate(it) }
    }
}
