package eu.kanade.presentation.more.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.powerManager
import tachiyomi.i18n.*

/** Installing extensions, posting notifications from Android 13, and running without battery optimisation. */
@Inject
class AndroidOnboardingPermissions : OnboardingPermissions {
    override val isOffered: Boolean = true

    @Composable
    override fun rememberRequests(): List<PermissionRequest> {
        val context = LocalContext.current
        val powerManager = context.powerManager

        var installGranted by remember {
            mutableStateOf(context.packageManager.canRequestPackageInstalls())
        }
        var notificationGranted by remember {
            mutableStateOf(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED,
            )
        }
        var batteryGranted by remember {
            mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
        }

        val installPermissionRequester = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                installGranted = context.packageManager.canRequestPackageInstalls()
            },
        )
        val batteryPermissionRequester = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                batteryGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            },
        )

        return buildList {
            add(
                PermissionRequest(
                    title = MR.strings.onboarding_permission_install_apps,
                    description = MR.strings.onboarding_permission_install_apps_description,
                    granted = installGranted,
                    request = {
                        installPermissionRequester.launch(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = "package:${context.packageName}".toUri()
                            },
                        )
                    },
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { notificationGranted = it },
                )
                add(
                    PermissionRequest(
                        title = MR.strings.onboarding_permission_notifications,
                        description = MR.strings.onboarding_permission_notifications_description,
                        granted = notificationGranted,
                        request = { permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    ),
                )
            }
            add(
                PermissionRequest(
                    title = MR.strings.onboarding_permission_ignore_battery_opts,
                    description = MR.strings.onboarding_permission_ignore_battery_opts_description,
                    granted = batteryGranted,
                    request = {
                        @SuppressLint("BatteryLife")
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        batteryPermissionRequester.launch(intent)
                    },
                ),
            )
        }
    }
}
