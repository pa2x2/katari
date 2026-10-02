package mihon.feature.appupdate.install

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.IntentSanitizer
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import kotlinx.coroutines.CompletableDeferred
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File

/**
 * Installs an update APK of this app through a PackageInstaller session, so the update never leaves the app for a
 * browser or file manager.
 *
 * On Android 12+ the session needs no confirmation once Katari is the installer of record, which it is after its first
 * self-update. Otherwise Android sends a confirmation screen, which is shown at once while Katari is in front and held
 * until it is back otherwise, since a background app can't start activities.
 *
 * A successful self-update kills the process, so [install] only returns for a cancelled install.
 */
internal class AppUpdateInstaller(private val context: Context) {

    private val packageInstaller = context.packageManager.packageInstaller
    private val statusAction = "${context.packageName}.APP_UPDATE_INSTALL_STATUS"

    private val lock = Any()

    /** The install in flight; guarded by [lock]. */
    private var pending: PendingInstall? = null

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onStatus(intent)
    }

    init {
        ContextCompat.registerReceiver(
            context,
            statusReceiver,
            IntentFilter(statusAction),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** The system page that allows installs from Katari ("Install unknown apps"). */
    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    suspend fun install(file: File): InstallOutcome {
        val outcome = CompletableDeferred<InstallOutcome>()
        synchronized(lock) {
            pending?.result?.completeExceptionally(
                AppUpdateException(AppUpdateError.InstallFailed("A newer install request replaced this one.")),
            )
            pending = null
        }
        // Sessions left behind by an earlier attempt or an earlier process would otherwise linger until reboot.
        packageInstaller.mySessions.forEach { runCatching { packageInstaller.abandonSession(it.sessionId) } }

        withIOContext { commit(file, outcome) }
        return outcome.await()
    }

    /** Shows a held confirmation screen, or notices that it was dismissed without a status broadcast. */
    fun onForeground() {
        synchronized(lock) {
            val install = pending ?: return
            val confirmation = install.deferredConfirmation
            if (confirmation != null) {
                pending = install.copy(deferredConfirmation = null, confirmationShown = true)
                context.startActivity(confirmation)
                return
            }
            // Some Android builds drop the status broadcast when the confirmation screen is cancelled.
            if (install.confirmationShown && packageInstaller.getSessionInfo(install.sessionId) == null) {
                settle(InstallOutcome.Cancelled)
            }
        }
    }

    private fun commit(file: File, outcome: CompletableDeferred<InstallOutcome>) {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(file.length())
            setInstallReason(PackageManager.INSTALL_REASON_USER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        var sessionId: Int? = null
        try {
            val id = packageInstaller.createSession(params)
            sessionId = id
            packageInstaller.openSession(id).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("base.apk", 0, file.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                synchronized(lock) { pending = PendingInstall(id, outcome) }
                @SuppressLint("RequestInstallPackagesPolicy")
                session.commit(statusIntent(id).intentSender)
            }
        } catch (e: Exception) {
            sessionId?.let { runCatching { packageInstaller.abandonSession(it) } }
            synchronized(lock) { if (pending?.result === outcome) pending = null }
            outcome.completeExceptionally(AppUpdateException(AppUpdateError.InstallFailed(e.message)))
        }
    }

    private fun onStatus(intent: Intent) {
        val id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        synchronized(lock) {
            val install = pending?.takeIf { it.sessionId == id } ?: return
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmation = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
                        ?.let { sanitize(it, id) }
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    when {
                        confirmation == null -> settle(
                            AppUpdateException(AppUpdateError.InstallFailed("Android sent no confirmation screen.")),
                        )
                        isInForeground() -> {
                            pending = install.copy(confirmationShown = true)
                            context.startActivity(confirmation)
                        }
                        else -> pending = install.copy(deferredConfirmation = confirmation)
                    }
                }
                PackageInstaller.STATUS_SUCCESS -> settle(InstallOutcome.Installed)
                PackageInstaller.STATUS_FAILURE_ABORTED -> settle(InstallOutcome.Cancelled)
                PackageInstaller.STATUS_FAILURE_STORAGE -> settle(
                    AppUpdateException(AppUpdateError.InsufficientStorage),
                )
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> settle(AppUpdateException(AppUpdateError.Incompatible))
                else -> settle(AppUpdateException(AppUpdateError.InstallFailed(message)))
            }
        }
    }

    /** Completes the install in flight; call with [lock] held. */
    private fun settle(outcome: InstallOutcome) {
        pending?.result?.complete(outcome)
        pending = null
    }

    private fun settle(error: AppUpdateException) {
        pending?.result?.completeExceptionally(error)
        pending = null
    }

    /** Lets only the confirmation screen for our own session through. */
    private fun sanitize(confirmation: Intent, sessionId: Int): Intent {
        return IntentSanitizer.Builder()
            .allowAction(confirmation.action.orEmpty())
            .allowExtra(PackageInstaller.EXTRA_SESSION_ID) { id -> id == sessionId }
            .allowAnyComponent()
            // The system installer's package differs between Android builds.
            .allowPackage { true }
            .build()
            .sanitizeByFiltering(confirmation)
    }

    private fun statusIntent(sessionId: Int): PendingIntent {
        val intent = Intent(statusAction).setPackage(context.packageName)
        // Mutable so PackageInstaller can add the status extras.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, sessionId, intent, flags)
    }

    private fun isInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private data class PendingInstall(
        val sessionId: Int,
        val result: CompletableDeferred<InstallOutcome>,
        /** A confirmation screen that arrived while Katari was in the background. */
        val deferredConfirmation: Intent? = null,
        val confirmationShown: Boolean = false,
    )
}

internal enum class InstallOutcome {
    /** Rarely seen: Android usually kills the process as it replaces the app. */
    Installed,
    Cancelled,
}
