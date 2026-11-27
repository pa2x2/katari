package app.katari.backupconversion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
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
 * "What comes over" (D023): one row per kind of data the backup holds, with its amount and bar. A kind that comes over
 * in part opens in place to what is left behind, then what comes over (D053), each thing with where it is and its
 * notes. A kind that comes over whole opens only when the report has notes on its things (D061), to what comes over.
 */
internal fun LazyListScope.conversionReportItems(
    report: ConversionReport,
    expanded: Set<Kind>,
    onToggle: (Kind) -> Unit,
) {
    item(key = "report-header") { RowGroupHeader(stringResource(MR.strings.backup_conversion_what_comes_over)) }
    rowGroupItems(report.rows, key = { it.kind }) { row ->
        val carried = row.carried.size
        val total = carried + row.leftBehind.size
        val partial = carried > 0 && row.leftBehind.isNotEmpty()
        val notedWhole = row.leftBehind.isEmpty() && row.carried.any { it.note != null }
        CoverageRow(
            title = stringResource(row.kind.title),
            amount = row.amount(),
            carried = carried,
            total = total,
            expanded = row.kind in expanded,
            onClick = { onToggle(row.kind) },
            details = if (partial || notedWhole) {
                { CoverageDetails(row) }
            } else {
                null
            },
        )
    }
    item(key = "report-gap") { RowGroupGap() }
}

@Composable
private fun CoverageDetails(row: ConversionReport.Row) {
    Column {
        if (row.leftBehind.isNotEmpty()) {
            RowGroupSubheader(stringResource(MR.strings.backup_conversion_left_behind))
            ItemRows(row.leftBehind)
        }
        RowGroupSubheader(stringResource(MR.strings.backup_conversion_comes_over))
        ItemRows(row.carried)
    }
}

/**
 * One row per thing, under it where it is and every note the report has for it, such as an entry's custom fields left
 * behind or each tracker's sign-in.
 */
@Composable
private fun ItemRows(items: List<ConversionReport.Item>) {
    items.groupBy { it.name }.forEach { (name, notes) ->
        val details = listOfNotNull(name.place()) + notes.mapNotNull { it.note }.distinct().map { stringResource(it) }
        SettingsRow(title = name.label(), subtitle = details.joinToString(" · ").ifEmpty { null })
    }
}

@Composable
private fun ConversionReport.Row.amount(): String {
    val carried = carried.size
    return when {
        leftBehind.isEmpty() -> stringResource(
            MR.strings.backup_conversion_amount_all,
            pluralStringResource(kind.count, carried, carried),
        )
        carried == 0 -> leftBehind.map { it.note }.distinct().singleOrNull()
            ?.let { stringResource(MR.strings.backup_conversion_amount_none_because, stringResource(it)) }
            ?: stringResource(MR.strings.backup_conversion_amount_none)
        else -> stringResource(
            MR.strings.backup_conversion_amount_part,
            carried,
            carried + leftBehind.size,
            leftBehind.size,
        )
    }
}

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
}

/** Where a setting is: in Katari for one that comes over, in the other app for one left behind (D053). */
@Composable
private fun ConversionReport.Name.place(): String? = when (this) {
    is ConversionReport.Name.Setting -> katariSettingNames[key]?.place?.map { stringResource(it) }
    is ConversionReport.Name.OtherAppSetting -> listOf(app) + place.map { stringResource(it) }
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
