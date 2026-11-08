package app.katari.missingextensions.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.missingextensions.install.MissingExtensionInstalls
import app.katari.missingextensions.model.MissingExtension
import app.katari.missingextensions.model.MissingExtensions
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
 * The missing-extensions sheet: what is installed but not trusted here, what can be installed, ticked to start with,
 * and what can't with why. An installable row stays once its extension is installed, saying so, until the sheet
 * closes; a trusted one goes once its extension loads. The model outlives the sheet, with the screen
 * that shows it, so [open] starts each opening afresh.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class MissingExtensionsSheetViewModel(
    missing: MissingExtensions,
    private val installs: MissingExtensionInstalls,
    private val extensions: ExtensionManagement,
    profileManager: ProfileManager,
) : ViewModel() {

    private val opening = MutableStateFlow(Opening())

    val state: StateFlow<State?> = combine(
        missing.all,
        installs.steps,
        opening,
        profileManager.profiles,
        profileManager.activeProfile,
    ) { all, steps, opening, profiles, activeProfile ->
        all ?: return@combine null
        val missingKeys = all.mapTo(HashSet()) { it.key }
        all.filter { it.isInstallable }.forEach { opening.shown[it.key] = it }
        State(
            untrusted = all.filter { it.untrusted != null },
            installable = opening.shown.values.map { extension ->
                InstallRow(
                    extension = extension,
                    ticked = extension.key !in opening.unticked,
                    step = steps[extension.key],
                    installed = extension.key !in missingKeys || steps[extension.key] == InstallStep.Installed,
                )
            },
            unavailable = all.filter { it.cantInstall != null },
            openProfileId = activeProfile?.id,
            profileNames = profiles.associate { it.id to it.name },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    fun open() {
        opening.value = Opening()
    }

    fun setTicked(key: String, ticked: Boolean) {
        opening.update { Opening(if (ticked) it.unticked - key else it.unticked + key, it.shown) }
    }

    fun install() {
        val rows = state.value?.installable ?: return
        installs.install(rows.filter { it.canStart }.mapNotNull { it.extension.listing })
    }

    fun trust(extension: MissingExtension) {
        extension.untrusted?.let(extensions::trust)
    }

    fun uninstall(extension: MissingExtension) {
        extension.untrusted?.let(extensions::uninstall)
    }

    /** One opening of the sheet; a new one compares unequal even to an untouched one, so the rows are rebuilt. */
    private class Opening(
        val unticked: Set<String> = emptySet(),
        /** Every installable extension shown since it opened, in the order they came, so installed ones keep a row. */
        val shown: LinkedHashMap<String, MissingExtension> = LinkedHashMap(),
    )

    @Immutable
    data class State(
        val untrusted: List<MissingExtension>,
        val installable: List<InstallRow>,
        val unavailable: List<MissingExtension>,
        val openProfileId: Long?,
        val profileNames: Map<Long, String>,
    ) {
        val toInstall: Int
            get() = installable.count { it.canStart }

        val isInstalling: Boolean
            get() = installable.any { it.step?.isCompleted() == false }

        val isRetry: Boolean
            get() = installable.any { it.canStart && it.step is InstallStep.Error }
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
            get() = ticked && !installed && step?.isCompleted() != false
    }
}
