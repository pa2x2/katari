package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.RocketLaunch
import app.katari.missingextensions.ui.MissingExtensionsNoticeViewModel
import app.katari.missingextensions.ui.MissingExtensionsSheet
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.GuidePageHeading
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowGroup
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.rememberSyncStatus
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreStatus
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The guide's last page: setup is done, with the getting started guide for someone new. A library that needs
 * extensions that aren't installed here, from a sync account or from the backup a guide [startedFromBackup] restored,
 * is offered those first, as the library arrives (D111).
 */
internal class FinishStep(private val startedFromBackup: Boolean) : OnboardingStep {

    @Composable
    override fun heading(): GuidePageHeading {
        val offer = extensionsOffer(startedFromBackup)
        return GuidePageHeading(
            icon = MaterialSymbols.Rounded.RocketLaunch,
            heading = stringResource(MR.strings.onboarding_finish_heading),
            subtitle = if (offer == null) {
                stringResource(MR.strings.onboarding_finish_subtitle, stringResource(MR.strings.app_name))
            } else if (offer.unchecked) {
                pluralStringResource(MR.plurals.onboarding_finish_stores_unreadable, offer.titles, offer.titles)
            } else {
                val plural = when (offer.from) {
                    LibraryOrigin.Account -> if (offer.libraryArriving) {
                        MR.plurals.onboarding_finish_arriving_missing
                    } else {
                        MR.plurals.onboarding_finish_account_missing
                    }
                    LibraryOrigin.Backup -> if (offer.libraryArriving) {
                        MR.plurals.onboarding_finish_restoring_missing
                    } else {
                        MR.plurals.onboarding_finish_restored_missing
                    }
                }
                pluralStringResource(plural, offer.titles, offer.titles)
            },
        )
    }

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val offer = extensionsOffer(startedFromBackup)
        val model = metroViewModel<MissingExtensionsNoticeViewModel>()
        var sheetShown by rememberSaveable { mutableStateOf(false) }
        RowGroup {
            row {
                ControlRow {
                    if (offer != null) {
                        Button(
                            label = stringResource(
                                if (offer.unchecked) MR.strings.action_retry else MR.strings.missing_extensions_title,
                            ),
                            onClick = { if (offer.unchecked) model.readStoresAgain() else sheetShown = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        label = stringResource(MR.strings.getting_started_guide),
                        onClick = { handler.openUri(GETTING_STARTED_URL) },
                        modifier = Modifier.fillMaxWidth(),
                        style = if (offer != null) ButtonStyle.Outlined else ButtonStyle.Filled,
                    )
                }
            }
        }
        if (sheetShown) {
            MissingExtensionsSheet(
                onDismissRequest = { sheetShown = false },
                onOpenStores = {
                    sheetShown = false
                    navigator.push(ExtensionStoresScreen())
                },
            )
        }
    }
}

/**
 * [titles] of the library [from] an account or a backup need extensions this device can install or trust, or, when
 * [unchecked], extensions a store that couldn't be read may list (D113); [libraryArriving] while a first sync still
 * downloads the library or the restore still runs, so the count still climbs.
 */
private class ExtensionsOffer(
    val titles: Int,
    val from: LibraryOrigin,
    val libraryArriving: Boolean,
    val unchecked: Boolean,
)

private enum class LibraryOrigin { Account, Backup }

/**
 * What the page offers a linked device, or one whose guide started from a backup, Katari's or another app's; null
 * otherwise or where its library needs nothing it can install.
 */
@Composable
private fun extensionsOffer(startedFromBackup: Boolean): ExtensionsOffer? {
    val linked = rememberSyncStatus()?.link is SyncStatus.Link.Linked
    val model = metroViewModel<MissingExtensionsNoticeViewModel>()
    val extensions by model.installable.collectAsStateWithLifecycle()
    val unchecked by model.unchecked.collectAsStateWithLifecycle()
    val arriving by model.libraryArriving.collectAsStateWithLifecycle()
    val restore by LocalAppUiGraph.current.backupRestoreStatus.state.collectAsStateWithLifecycle()
    val restoring = restore is BackupRestoreStatus.State.Running
    val installableTitles = extensions.sumOf { it.titleCount }.toInt()
    val uncheckedTitles = unchecked.sumOf { it.titleCount }.toInt()
    val isUnchecked = installableTitles == 0
    val titles = if (isUnchecked) uncheckedTitles else installableTitles
    return when {
        titles == 0 -> null
        linked -> ExtensionsOffer(titles, LibraryOrigin.Account, libraryArriving = arriving == true, isUnchecked)
        startedFromBackup -> ExtensionsOffer(titles, LibraryOrigin.Backup, libraryArriving = restoring, isUnchecked)
        else -> null
    }
}

const val GETTING_STARTED_URL = "${Constants.URL_DOCS}first-steps/"
