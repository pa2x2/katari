package eu.kanade.tachiyomi.extension.installer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentSanitizer
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.UserActionBehavior
import eu.kanade.tachiyomi.util.lang.use
import eu.kanade.tachiyomi.util.system.getParcelableExtraCompat
import eu.kanade.tachiyomi.util.system.getUriSize
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

internal class PackageInstallerInstaller(
    private val service: Service,
) : Installer(service) {

    private val packageInstaller = service.packageManager.packageInstaller

    private val packageActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    if (activeSession?.first?.userActionBehavior == UserActionBehavior.MarkAsRequiresUserAction) {
                        activeSession?.second?.let(packageInstaller::abandonSession)
                        continueQueue(InstallStep.RequiresUserAction)
                        return
                    }
                    val userAction = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
                        ?.run {
                            // Doesn't actually needed as the receiver is actually not exported
                            // But the warnings can't be suppressed without this
                            IntentSanitizer.Builder()
                                .allowAction(this.action!!)
                                .allowExtra(PackageInstaller.EXTRA_SESSION_ID) { id -> id == activeSession?.second }
                                .allowAnyComponent()
                                .allowPackage {
                                    // There is no way to check the actual installer name so allow all.
                                    true
                                }
                                .build()
                                .sanitizeByFiltering(this)
                        }
                    if (userAction == null) {
                        logcat(LogPriority.ERROR) { "Fatal error for $intent" }
                        continueQueue(InstallStep.Error("The system installer asked for an action it didn't provide"))
                        return
                    }
                    userAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        service.startActivity(userAction)
                    } catch (e: ActivityNotFoundException) {
                        // Some devices have the system installer's confirmation screen disabled or removed
                        logcat(LogPriority.WARN, e) { "Nothing can confirm the install" }
                        activeSession?.let { (_, sessionId) ->
                            runCatching { packageInstaller.abandonSession(sessionId) }
                        }
                        continueQueue(InstallStep.Error("Nothing on this device can confirm the install"))
                    }
                }
                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    continueQueue(InstallStep.Idle)
                }
                PackageInstaller.STATUS_SUCCESS -> continueQueue(InstallStep.Installed)
                else -> continueQueue(
                    InstallStep.Error(
                        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            ?: "The system installer failed ($status)",
                    ),
                )
            }
        }
    }

    @Volatile
    private var activeSession: Pair<Entry, Int>? = null

    // Set before the session is looked up, while the install publishes its session before checking this, so one of
    // the two always sees the other and abandons the session
    @Volatile
    private var cancelledEntry: Entry? = null

    // Always ready
    override var ready = true

    override fun processEntry(entry: Entry) {
        super.processEntry(entry)
        activeSession = null
        installScope.launch {
            var sessionId: Int? = null
            try {
                val installParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    installParams.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                val fileSize = service.getUriSize(entry.uri) ?: throw IllegalStateException()
                installParams.setSize(fileSize)
                sessionId = packageInstaller.createSession(installParams)
                activeSession = entry to sessionId
                check(cancelledEntry != entry) { "Cancelled before the copy started" }

                val inputStream = service.contentResolver.openInputStream(entry.uri) ?: throw IllegalStateException()
                val session = packageInstaller.openSession(sessionId)
                val outputStream = session.openWrite(entry.downloadId.toString(), 0, fileSize)
                session.use {
                    arrayOf(inputStream, outputStream).use {
                        inputStream.copyTo(outputStream)
                        session.fsync(outputStream)
                    }
                    service.contentResolver.delete(entry.uri, null, null)

                    val intentSender = PendingIntent.getBroadcast(
                        service,
                        sessionId,
                        Intent(INSTALL_ACTION).setPackage(service.packageName),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0,
                    ).intentSender
                    @SuppressLint("RequestInstallPackagesPolicy")
                    session.commit(intentSender)
                }
            } catch (e: Exception) {
                val cancelled = cancelledEntry == entry
                if (!cancelled) {
                    logcat(LogPriority.ERROR, e) { "Failed to install extension ${entry.downloadId} ${entry.uri}" }
                }
                // Already abandoned when the entry was cancelled mid-copy
                sessionId?.let { runCatching { packageInstaller.abandonSession(it) } }
                service.contentResolver.delete(entry.uri, null, null)
                if (getActiveEntry() == entry) {
                    continueQueue(if (cancelled) InstallStep.Idle else InstallStep.Error.from(e))
                }
            }
        }
    }

    // The active entry ends through its own install, which a cancel cuts short by abandoning the session
    override fun cancelEntry(entry: Entry): Boolean {
        if (getActiveEntry() != entry) return true
        cancelledEntry = entry
        val (sessionEntry, sessionId) = activeSession ?: return false
        if (sessionEntry != entry) return false
        return try {
            packageInstaller.abandonSession(sessionId)
            false
        } catch (_: SecurityException) {
            // Highly likely the session has succeeded
            true
        }
    }

    override fun onDestroy() {
        service.unregisterReceiver(packageActionReceiver)
        super.onDestroy()
    }

    init {
        ContextCompat.registerReceiver(
            service,
            packageActionReceiver,
            IntentFilter(INSTALL_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}

private const val INSTALL_ACTION = "PackageInstallerInstaller.INSTALL_ACTION"
