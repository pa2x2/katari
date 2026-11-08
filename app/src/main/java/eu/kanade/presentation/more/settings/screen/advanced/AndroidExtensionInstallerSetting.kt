package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import app.katari.presentation.kit.dialog.ConfirmDialog
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The APK installers Android can install extensions with, and the Shizuku app one of them needs. */
@Inject
class AndroidExtensionInstallerSetting(private val basePreferences: BasePreferences) : ExtensionInstallerSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any> {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val extensionInstallerPref = basePreferences.extensionInstaller
        var shizukuMissing by rememberSaveable { mutableStateOf(false) }

        if (shizukuMissing) {
            val dismiss = { shizukuMissing = false }
            ConfirmDialog(
                title = stringResource(MR.strings.ext_installer_shizuku),
                text = stringResource(MR.strings.ext_installer_shizuku_unavailable_dialog),
                confirmLabel = stringResource(MR.strings.action_download),
                onConfirm = {
                    dismiss()
                    uriHandler.openUri("https://shizuku.rikka.app/download")
                },
                onDismissRequest = dismiss,
            )
        }
        return Preference.PreferenceItem.ListPreference(
            preference = extensionInstallerPref,
            entries = extensionInstallerPref.entries
                .filter {
                    // TODO: allow private option in stable versions once URL handling is more fleshed out
                    if (isReleaseBuildType) {
                        it != BasePreferences.ExtensionInstaller.PRIVATE
                    } else {
                        true
                    }
                }
                .associateWith { stringResource(it.titleRes) },
            title = stringResource(MR.strings.ext_installer_pref),
            onValueChanged = {
                if (it == BasePreferences.ExtensionInstaller.SHIZUKU &&
                    !context.isShizukuInstalled
                ) {
                    shizukuMissing = true
                    false
                } else {
                    true
                }
            },
        )
    }
}
