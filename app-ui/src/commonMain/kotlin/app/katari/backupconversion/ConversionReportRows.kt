package app.katari.backupconversion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import app.katari.backupconversion.BringOverViewModel.StoreCheck
import app.katari.missingextensions.model.CantInstall
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.NoResults
import app.katari.presentation.kit.list.CoverageRow
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupSubheader
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Kind
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * "What comes over" (D023): one row per kind of data the backup holds, with its amount and bar. A kind with things left
 * behind opens in place to them, then to what comes over (D053, D076), each thing with where it is and its notes. A
 * kind that comes over whole opens only when the report has notes on its things (D061), to what comes over, except the
 * kinds that mix different things, which always open (D117).
 */
internal fun LazyListScope.conversionReportItems(
    report: ConversionReport,
    stores: StoreCheck?,
    expanded: Set<Kind>,
    onToggle: (Kind) -> Unit,
    onCheckStoresAgain: () -> Unit,
) {
    item(key = "report-header") { RowGroupHeader(stringResource(MR.strings.backup_conversion_what_comes_over)) }
    if (report.rows.isEmpty()) {
        item(key = "report-empty") { NoResults(message = stringResource(MR.strings.backup_conversion_nothing)) }
    }
    rowGroupItems(report.rows, key = { it.kind }) { reportRow ->
        val row = reportRow.withStores(stores)
        val unchecked = stores is StoreCheck.Checked && reportRow.sources().any {
            stores.cantInstall[it] == CantInstall.STORES_UNREADABLE
        }
        val carried = row.carried.thingCount()
        val total = carried + row.leftBehind.thingCount()
        val notedWhole = row.leftBehind.isEmpty() && row.carried.any { it.note != null }
        val opens = row.leftBehind.isNotEmpty() || notedWhole || row.kind in KINDS_OF_MIXED_THINGS
        CoverageRow(
            title = stringResource(row.kind.title),
            amount = row.amount(),
            carried = carried,
            total = total,
            expanded = row.kind in expanded,
            onClick = { onToggle(row.kind) },
            details = if (opens) {
                { CoverageDetails(row, onCheckStoresAgain.takeIf { unchecked }) }
            } else {
                null
            },
        )
    }
    item(key = "report-gap") { RowGroupGap() }
}

// Their titles don't say what they hold, such as saved searches and feeds, or custom titles and covers
private val KINDS_OF_MIXED_THINGS = setOf(Kind.APP_SPECIFIC, Kind.CUSTOM_INFO)

/** [onCheckStoresAgain] when a store its sources were looked up in couldn't be read (D113). */
@Composable
private fun CoverageDetails(row: ConversionReport.Row, onCheckStoresAgain: (() -> Unit)?) {
    Column {
        if (row.leftBehind.isNotEmpty()) {
            RowGroupSubheader(stringResource(MR.strings.backup_conversion_left_behind))
            ItemRows(row.leftBehind)
        }
        if (row.carried.isNotEmpty()) {
            RowGroupSubheader(stringResource(MR.strings.backup_conversion_comes_over))
            ItemRows(row.carried)
        }
        if (onCheckStoresAgain != null) {
            Button(
                label = stringResource(MR.strings.action_retry),
                onClick = onCheckStoresAgain,
                style = ButtonStyle.Text,
            )
        }
    }
}

private fun ConversionReport.Row.sources() =
    (carried + leftBehind).mapNotNull { (it.name as? ConversionReport.Name.Source)?.identity }

/**
 * The row with its sources' notes saying what the stores have for them, once they have been asked (D106), and the
 * sources named as the stores and Katari name them (D114).
 */
private fun ConversionReport.Row.withStores(stores: StoreCheck?): ConversionReport.Row {
    if (stores == null) return this
    fun List<ConversionReport.Item>.checked() = map { item ->
        val source = item.name as? ConversionReport.Name.Source ?: return@map item
        val note = when (stores) {
            StoreCheck.Checking -> ConversionReport.Note(MR.strings.backup_conversion_checking_stores)
            is StoreCheck.Checked -> when (stores.cantInstall[source.identity]) {
                null -> item.note
                CantInstall.NOT_LISTED -> ConversionReport.Note(MR.strings.backup_conversion_no_store)
                CantInstall.STORES_UNREADABLE -> ConversionReport.Note(MR.strings.backup_conversion_stores_unreadable)
                CantInstall.TORRENT -> ConversionReport.Note(MR.strings.backup_conversion_torrent_extension)
                CantInstall.INCOMPATIBLE -> ConversionReport.Note(MR.strings.backup_conversion_incompatible_extension)
            }
        }
        val name = (stores as? StoreCheck.Checked)?.names
            ?.takeIf { source.identity in it }
            ?.let { names -> source.copy(text = names[source.identity]) }
            ?: source
        item.copy(name = name, note = note)
    }
    return copy(carried = carried.checked(), leftBehind = leftBehind.checked())
}

/**
 * One row per thing, under it where it is and every note the report has for it, such as an entry's custom fields left
 * behind or each tracker's sign-in.
 */
@Composable
private fun ItemRows(items: List<ConversionReport.Item>) {
    items.groupBy { it.name.shown }.values.forEach { notes ->
        val name = notes.first().name
        val details = listOfNotNull(name.place()) + notes.mapNotNull { it.note }.distinct().map { it.shown() }
        SettingsRow(title = name.label(), subtitle = details.joinToString(" · ").ifEmpty { null })
    }
}

/**
 * What a name shows as, so items shown alike are listed as one thing with all their notes: an entry with a note for each
 * tracker, or a choice Katari keeps under two keys, as the chapter sort and its direction, with one title and place.
 */
private val ConversionReport.Name.shown: Any
    get() = when (this) {
        is ConversionReport.Name.Setting -> katariSettingNames[key]?.let { it.title to it.place } ?: key
        else -> this
    }

/** How many things the items are: each setting once, however many keys it's kept under; each other item on its own. */
private fun List<ConversionReport.Item>.thingCount(): Int {
    val settings = filter { it.name is ConversionReport.Name.Setting }
    return size - settings.size + settings.distinctBy { it.name.shown }.size
}

@Composable
private fun ConversionReport.Row.amount(): String {
    val carriedCount = carried.thingCount()
    val leftBehindCount = leftBehind.thingCount()
    return when {
        leftBehindCount == 0 -> stringResource(
            MR.strings.backup_conversion_amount_all,
            pluralStringResource(kind.count, carriedCount, carriedCount),
        )
        carriedCount == 0 -> leftBehind.map { it.note }.distinct().singleOrNull()
            ?.let { stringResource(MR.strings.backup_conversion_amount_none_because, it.shown()) }
            ?: stringResource(MR.strings.backup_conversion_amount_none)
        else -> stringResource(
            MR.strings.backup_conversion_amount_part,
            carriedCount,
            carriedCount + leftBehindCount,
            leftBehindCount,
        )
    }
}

@Composable
private fun ConversionReport.Note.shown(): String = stringResource(text, *args.toTypedArray())

/**
 * What the report calls a thing: its own name, a Katari setting by Katari's title (D043), a setting of the other app
 * by that app's name for it (D055), or a part the tool names. A key the table doesn't name shows as it is.
 */
@Composable
internal fun ConversionReport.Name.label(): String = when (this) {
    is ConversionReport.Name.Text -> text
    is ConversionReport.Name.Setting -> katariSettingNames[key]?.let { stringResource(it.title) } ?: key
    is ConversionReport.Name.OtherAppSetting -> stringResource(label)
    is ConversionReport.Name.Part -> stringResource(label)
    is ConversionReport.Name.Source -> text ?: stringResource(MR.strings.backup_source_unnamed)
}

/**
 * Where a setting is: in Katari for one that comes over, in the other app for one left behind (D053); an unnamed
 * source's number.
 */
@Composable
private fun ConversionReport.Name.place(): String? = when (this) {
    is ConversionReport.Name.Setting -> katariSettingNames[key]?.place?.map { stringResource(it) }
    is ConversionReport.Name.OtherAppSetting -> listOf(app) + place.map { stringResource(it) }
    // An unnamed source by its number, so two of them can be told apart (D114)
    is ConversionReport.Name.Source -> listOf(identity.externalId.toString()).takeIf { text == null }
    is ConversionReport.Name.Text, is ConversionReport.Name.Part -> null
}?.reduceOrNull { outer, inner -> stringResource(MR.strings.backup_conversion_place, outer, inner) }

private val Kind.title: StringResource
    get() = when (this) {
        Kind.LIBRARY -> MR.strings.backup_conversion_kind_library
        Kind.CATEGORIES -> MR.strings.backup_conversion_kind_categories
        Kind.PROGRESS_AND_HISTORY -> MR.strings.backup_conversion_kind_progress
        Kind.MERGED_ENTRIES -> MR.strings.backup_conversion_kind_merged
        Kind.CUSTOM_INFO -> MR.strings.backup_conversion_kind_custom_info
        Kind.APP_SETTINGS -> MR.strings.backup_conversion_kind_app_settings
        Kind.EXTENSION_SETTINGS -> MR.strings.backup_conversion_kind_extension_settings
        Kind.EXTENSION_STORES -> MR.strings.backup_conversion_kind_extension_stores
        Kind.INSTALLED_EXTENSIONS -> MR.strings.backup_conversion_kind_installed_extensions
        Kind.TRACKERS -> MR.strings.backup_conversion_kind_trackers
        Kind.APP_SPECIFIC -> MR.strings.backup_conversion_kind_other
    }

/** How the kind's things are counted: "All 312 entries". */
private val Kind.count: PluralsResource
    get() = when (this) {
        Kind.LIBRARY, Kind.PROGRESS_AND_HISTORY, Kind.MERGED_ENTRIES, Kind.TRACKERS ->
            MR.plurals.backup_conversion_count_entries
        Kind.CATEGORIES -> MR.plurals.backup_conversion_count_categories
        Kind.APP_SETTINGS -> MR.plurals.backup_conversion_count_settings
        Kind.EXTENSION_SETTINGS -> MR.plurals.backup_conversion_count_sources
        Kind.EXTENSION_STORES -> MR.plurals.backup_conversion_count_stores
        Kind.INSTALLED_EXTENSIONS -> MR.plurals.backup_conversion_count_extensions
        Kind.CUSTOM_INFO, Kind.APP_SPECIFIC -> MR.plurals.backup_conversion_count_items
    }
