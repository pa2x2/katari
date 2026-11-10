package app.katari.missingextensions.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.missingextensions.install.MissingExtensionInstalls
import app.katari.missingextensions.model.MissingExtension
import app.katari.missingextensions.model.MissingExtensions
import app.katari.missingextensions.otherdevices.OtherDeviceChanges
import app.katari.missingextensions.otherdevices.OtherDeviceExtension
import app.katari.missingextensions.otherdevices.OtherDeviceSuggestions
import app.katari.profiles.core.ProfileManager
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

/**
 * The sheet of extensions for this device: what the library needs that is installed but not trusted here, what it
 * needs that can be installed, ticked to start with, and what can't with why; then what the account's other devices
 * installed, ticked, and uninstalled, ticked unless titles here use it. An installable row stays once its extension
 * is installed, saying so, until the sheet closes; a trusted or uninstalled one goes once that is done. The model
 * outlives the sheet, with the screen that shows it, so [open] starts each opening afresh.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class MissingExtensionsSheetViewModel(
    missing: MissingExtensions,
    private val otherDevices: OtherDeviceSuggestions,
    private val installs: MissingExtensionInstalls,
    private val extensions: ExtensionManagement,
    profileManager: ProfileManager,
) : ViewModel() {

    private val opening = MutableStateFlow(Opening())

    val state: StateFlow<State?> = combine(
        missing.all,
        otherDevices.changes,
        installs.steps,
        opening,
        combine(profileManager.profiles, profileManager.activeProfile, ::Pair),
    ) { all, otherChanges, steps, opening, (profiles, activeProfile) ->
        all ?: return@combine null
        val changes = otherChanges ?: OtherDeviceChanges.NONE
        val missingKeys = all.mapTo(HashSet()) { it.key }
        all.filter { it.isInstallable }.forEach { opening.shown[it.key] = it }
        val offeredKeys = changes.installs.mapTo(HashSet()) { it.pkgName }
        changes.installs.forEach { opening.shownOther[it.pkgName] = it }
        State(
            untrusted = all.filter { it.untrusted != null },
            installable = opening.shown.values.map { extension ->
                InstallRow(
                    extension = extension,
                    ticked = opening.ticked(extension.key, default = true),
                    step = steps[extension.key],
                    installed = extension.key !in missingKeys || steps[extension.key] == InstallStep.Installed,
                )
            },
            unavailable = all.filter { it.cantInstall != null },
            otherInstalls = opening.shownOther.values.map { extension ->
                OtherInstallRow(
                    extension = extension,
                    ticked = opening.ticked(extension.pkgName, default = true),
                    step = steps[extension.pkgName],
                    installed = extension.pkgName !in offeredKeys ||
                        steps[extension.pkgName] == InstallStep.Installed,
                )
            },
            otherUninstalls = changes.uninstalls.map { extension ->
                UninstallRow(
                    extension = extension,
                    ticked = opening.ticked(UNINSTALL + extension.pkgName, default = extension.titleCount == 0L),
                )
            },
            openProfileId = activeProfile?.id,
            profileNames = profiles.associate { it.id to it.name },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    fun open() {
        opening.value = Opening()
    }

    fun setTicked(key: String, ticked: Boolean) {
        opening.update { it.with(key, ticked) }
    }

    fun setUninstallTicked(pkgName: String, ticked: Boolean) = setTicked(UNINSTALL + pkgName, ticked)

    fun install() {
        val current = state.value ?: return
        installs.install(
            current.installable.filter { it.canStart }.mapNotNull { it.extension.listing } +
                current.otherInstalls.filter { it.canStart }.mapNotNull { it.extension.listing },
        )
    }

    fun trust(extension: MissingExtension) {
        extension.untrusted?.let(extensions::trust)
    }

    fun uninstall(extension: MissingExtension) {
        extension.untrusted?.let(extensions::uninstall)
    }

    /** Uninstalls the ticked extensions the other devices uninstalled. */
    fun uninstallOffered() {
        otherDevices.uninstall(state.value?.otherUninstalls.orEmpty().filter { it.ticked }.map { it.extension })
    }

    /** The unticked installs of the other devices are never offered again. */
    fun declineInstalls() {
        otherDevices.declineInstalls(
            state.value?.otherInstalls.orEmpty().filter { !it.ticked && !it.installed }.map { it.extension },
        )
    }

    /** The unticked uninstalls of the other devices are never offered again. */
    fun declineUninstalls() {
        otherDevices.declineUninstalls(
            state.value?.otherUninstalls.orEmpty().filter {
                !it.ticked
            }.map { it.extension },
        )
    }

    /** One opening of the sheet; a new one compares unequal even to an untouched one, so the rows are rebuilt. */
    private class Opening(
        /** The rows ticked or unticked since it opened, by key; the others keep their row's default. */
        val ticks: Map<String, Boolean> = emptyMap(),
        /** Every installable extension shown since it opened, in the order they came, so installed ones keep a row. */
        val shown: LinkedHashMap<String, MissingExtension> = LinkedHashMap(),
        /** The same for the other devices' installs. */
        val shownOther: LinkedHashMap<String, OtherDeviceExtension> = LinkedHashMap(),
    ) {
        fun ticked(key: String, default: Boolean): Boolean = ticks[key] ?: default

        fun with(key: String, ticked: Boolean) = Opening(ticks + (key to ticked), shown, shownOther)
    }

    @Immutable
    data class State(
        val untrusted: List<MissingExtension>,
        val installable: List<InstallRow>,
        val unavailable: List<MissingExtension>,
        val otherInstalls: List<OtherInstallRow>,
        val otherUninstalls: List<UninstallRow>,
        val openProfileId: Long?,
        val profileNames: Map<Long, String>,
    ) {
        /** Whether anything the library's titles need is shown. */
        val hasMissing: Boolean
            get() = untrusted.isNotEmpty() || installable.isNotEmpty() || unavailable.isNotEmpty()

        val toInstall: Int
            get() = installable.count { it.canStart } + otherInstalls.count { it.canStart }

        val toUninstall: Int
            get() = otherUninstalls.count { it.ticked }

        val isInstalling: Boolean
            get() = installable.any { it.step?.isCompleted() == false } ||
                otherInstalls.any { it.step?.isCompleted() == false }

        val isRetry: Boolean
            get() = installable.any { it.canStart && it.step is InstallStep.Error } ||
                otherInstalls.any { it.canStart && it.step is InstallStep.Error }
    }

    @Immutable
    data class InstallRow(
        val extension: MissingExtension,
        val ticked: Boolean,
        /** Null until it's queued from here. */
        val step: InstallStep?,
        val installed: Boolean,
    ) {
        /** Ticked and neither installed nor on its way, or failed and so worth another try. */
        val canStart: Boolean
            get() = canStartInstall(ticked, installed, step)
    }

    @Immutable
    data class OtherInstallRow(
        val extension: OtherDeviceExtension,
        val ticked: Boolean,
        /** Null until it's queued from here. */
        val step: InstallStep?,
        val installed: Boolean,
    ) {
        val canStart: Boolean
            get() = canStartInstall(ticked, installed, step)
    }

    @Immutable
    data class UninstallRow(val extension: OtherDeviceExtension, val ticked: Boolean)

    private companion object {
        // Ticks of uninstall rows are kept apart from install rows', whose keys are the same package names
        const val UNINSTALL = "uninstall:"
    }
}

/** Ticked and neither installed nor on its way, or failed and so worth another try. */
private fun canStartInstall(ticked: Boolean, installed: Boolean, step: InstallStep?): Boolean =
    ticked && !installed && step?.isCompleted() != false
