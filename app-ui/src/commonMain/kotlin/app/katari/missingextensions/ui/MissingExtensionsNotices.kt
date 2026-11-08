package app.katari.missingextensions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Extension
import app.katari.missingextensions.model.MissingExtension
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.ui.di.LocalAppUiGraph
import dev.zacsweers.metrox.viewmodel.metroViewModel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Library's suggestion to install the extensions its titles need, shown once for each extension that becomes
 * needed; Not now and Review both put it away until another one is.
 */
@Composable
fun MissingExtensionsLibraryNotice(onReview: () -> Unit, modifier: Modifier = Modifier) {
    val model = metroViewModel<MissingExtensionsNoticeViewModel>()
    val extensions by model.libraryCard.collectAsStateWithLifecycle()
    if (extensions.isEmpty()) return
    NoticeCard(
        text = noticeText(extensions),
        tone = NoticeTone.Info,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Extension,
        actionLabel = stringResource(MR.strings.action_review),
        onAction = {
            model.answerLibraryCard()
            onReview()
        },
        dismissLabel = stringResource(MR.strings.action_not_now),
        onDismiss = model::answerLibraryCard,
    )
}

/** Browse → Extensions' way back to the sheet, shown while anything can be installed, whatever the Library card did. */
@Composable
fun MissingExtensionsNotice(onReview: () -> Unit, modifier: Modifier = Modifier) {
    val model = metroViewModel<MissingExtensionsNoticeViewModel>()
    val extensions by model.installable.collectAsStateWithLifecycle()
    if (extensions.isEmpty()) return
    NoticeCard(
        text = noticeText(extensions),
        tone = NoticeTone.Info,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Extension,
        actionLabel = stringResource(MR.strings.action_review),
        onAction = onReview,
    )
}

/**
 * How many titles need [extensions]. Extensions serve every profile, so the count does too, and says so when some of
 * the titles are in profiles other than the open one.
 */
@Composable
internal fun noticeText(extensions: List<MissingExtension>): String {
    val profileManager = LocalAppUiGraph.current.profileManager
    val openProfile by profileManager.activeProfile.collectAsStateWithLifecycle()
    val titles = extensions.sumOf { it.titleCount }.toInt()
    val beyondOpenProfile = extensions.any { extension -> extension.countsByProfile.keys.any { it != openProfile?.id } }
    return pluralStringResource(
        if (beyondOpenProfile) MR.plurals.missing_extensions_notice_profiles else MR.plurals.missing_extensions_notice,
        titles,
        titles,
    )
}
