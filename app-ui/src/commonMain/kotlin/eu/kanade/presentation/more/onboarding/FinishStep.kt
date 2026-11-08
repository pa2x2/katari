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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The guide's last page: setup is done, with the getting started guide for someone new. A device linked to a sync
 * account whose library needs extensions that aren't installed here is offered those first, as its library arrives.
 */
internal class FinishStep : OnboardingStep {

    @Composable
    override fun heading(): GuidePageHeading {
        val offer = extensionsOffer()
        return GuidePageHeading(
            icon = MaterialSymbols.Rounded.RocketLaunch,
            heading = stringResource(MR.strings.onboarding_finish_heading),
            subtitle = when {
                offer == null ->
                    stringResource(MR.strings.onboarding_finish_subtitle, stringResource(MR.strings.app_name))
                offer.libraryArriving ->
                    pluralStringResource(MR.plurals.onboarding_finish_arriving_missing, offer.titles, offer.titles)
                else -> pluralStringResource(MR.plurals.onboarding_finish_account_missing, offer.titles, offer.titles)
            },
        )
    }

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val offer = extensionsOffer()
        var sheetShown by rememberSaveable { mutableStateOf(false) }
        RowGroup {
            row {
                ControlRow {
                    if (offer != null) {
                        Button(
                            label = stringResource(MR.strings.missing_extensions_title),
                            onClick = { sheetShown = true },
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
 * [titles] of the account's library need extensions this device can install or trust; [libraryArriving] while a first
 * sync still downloads the library, so the count still climbs.
 */
private class ExtensionsOffer(val titles: Int, val libraryArriving: Boolean)

/** What the page offers a linked device; null where it isn't linked or its library needs nothing it can install. */
@Composable
private fun extensionsOffer(): ExtensionsOffer? {
    val linked = rememberSyncStatus()?.link is SyncStatus.Link.Linked
    val model = metroViewModel<MissingExtensionsNoticeViewModel>()
    val extensions by model.installable.collectAsStateWithLifecycle()
    val arriving by model.libraryArriving.collectAsStateWithLifecycle()
    val titles = extensions.sumOf { it.titleCount }.toInt()
    return ExtensionsOffer(titles, libraryArriving = arriving == true).takeIf { linked && titles > 0 }
}

const val GETTING_STARTED_URL = "${Constants.URL_DOCS}first-steps/"
