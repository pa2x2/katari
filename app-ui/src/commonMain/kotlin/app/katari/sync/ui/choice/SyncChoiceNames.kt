package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.ChromeReaderMode
import app.katari.icons.materialsymbols.automirroredrounded.Label
import app.katari.icons.materialsymbols.rounded.CollectionsBookmark
import app.katari.icons.materialsymbols.rounded.Explore
import app.katari.icons.materialsymbols.rounded.Extension
import app.katari.icons.materialsymbols.rounded.History
import app.katari.icons.materialsymbols.rounded.Image
import app.katari.icons.materialsymbols.rounded.Palette
import app.katari.icons.materialsymbols.rounded.PlayCircle
import app.katari.icons.materialsymbols.rounded.QueryStats
import app.katari.icons.materialsymbols.rounded.RecordVoiceOver
import app.katari.icons.materialsymbols.rounded.Security
import app.katari.icons.materialsymbols.rounded.TextFields
import app.katari.icons.materialsymbols.rounded.Translate
import app.katari.icons.materialsymbols.rounded.Tune
import app.katari.icons.materialsymbols.rounded.Update
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.setting.scope.DeviceBound
import app.katari.sync.client.setting.scope.SettingsArea
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How What syncs names a data group. */
@Composable
internal fun DataGroup.title(): String = stringResource(
    when (this) {
        DataGroup.HISTORY -> MR.strings.sync_choice_history
        DataGroup.STATISTICS -> MR.strings.sync_choice_statistics
        DataGroup.TITLE_CHOICES -> MR.strings.sync_choice_title_choices
        DataGroup.EXTENSION_REPOSITORIES -> MR.strings.sync_choice_repositories
        DataGroup.FONTS -> MR.strings.sync_choice_fonts
        DataGroup.COVERS -> MR.strings.sync_choice_covers
        DataGroup.CATEGORIES -> MR.strings.sync_choice_categories
    },
)

@Composable
internal fun DataGroup.summary(): String? = when (this) {
    DataGroup.HISTORY -> MR.strings.sync_choice_history_summary
    DataGroup.STATISTICS -> MR.strings.sync_choice_statistics_summary
    DataGroup.TITLE_CHOICES -> MR.strings.sync_choice_title_choices_summary
    DataGroup.FONTS -> MR.strings.sync_choice_fonts_summary
    DataGroup.CATEGORIES -> MR.strings.sync_choice_categories_summary
    DataGroup.EXTENSION_REPOSITORIES, DataGroup.COVERS -> null
}?.let { stringResource(it) }

internal val DataGroup.icon: ImageVector
    get() = when (this) {
        DataGroup.HISTORY -> MaterialSymbols.Rounded.History
        DataGroup.STATISTICS -> MaterialSymbols.Rounded.QueryStats
        DataGroup.TITLE_CHOICES -> MaterialSymbols.Rounded.Tune
        DataGroup.EXTENSION_REPOSITORIES -> MaterialSymbols.Rounded.Extension
        DataGroup.FONTS -> MaterialSymbols.Rounded.TextFields
        DataGroup.COVERS -> MaterialSymbols.Rounded.Image
        DataGroup.CATEGORIES -> MaterialSymbols.AutoMirroredRounded.Label
    }

/** How What syncs names a Settings area: as Settings names the screen it is, where it is one. */
internal val SettingsArea.titleRes: StringResource
    get() = when (this) {
        SettingsArea.APPEARANCE -> MR.strings.pref_category_appearance
        SettingsArea.LIBRARY -> MR.strings.pref_category_library
        SettingsArea.LIBRARY_UPDATES -> MR.strings.pref_category_library_updates
        SettingsArea.READERS -> MR.strings.pref_category_readers
        SettingsArea.PLAYERS -> MR.strings.pref_category_players
        SettingsArea.TRANSLATION -> MR.strings.translation_title
        SettingsArea.TEXT_TO_SPEECH -> MR.strings.tts_title
        SettingsArea.BROWSE -> MR.strings.browse
        SettingsArea.SECURITY -> MR.strings.pref_category_security
        SettingsArea.EXTENSION_SETTINGS -> MR.strings.sync_choice_extension_settings
        SettingsArea.STATISTICS -> MR.strings.sync_choice_statistics
    }

@Composable
internal fun SettingsArea.title(): String = stringResource(titleRes)

internal val SettingsArea.icon: ImageVector
    get() = when (this) {
        SettingsArea.APPEARANCE -> MaterialSymbols.Rounded.Palette
        SettingsArea.LIBRARY -> MaterialSymbols.Rounded.CollectionsBookmark
        SettingsArea.LIBRARY_UPDATES -> MaterialSymbols.Rounded.Update
        SettingsArea.READERS -> MaterialSymbols.AutoMirroredRounded.ChromeReaderMode
        SettingsArea.PLAYERS -> MaterialSymbols.Rounded.PlayCircle
        SettingsArea.TRANSLATION -> MaterialSymbols.Rounded.Translate
        SettingsArea.TEXT_TO_SPEECH -> MaterialSymbols.Rounded.RecordVoiceOver
        SettingsArea.BROWSE -> MaterialSymbols.Rounded.Explore
        SettingsArea.SECURITY -> MaterialSymbols.Rounded.Security
        SettingsArea.EXTENSION_SETTINGS -> MaterialSymbols.Rounded.Extension
        SettingsArea.STATISTICS -> MaterialSymbols.Rounded.QueryStats
    }

/** Why a setting only works on the device it was made on. */
@Composable
internal fun DeviceBound.reason(): String = stringResource(
    when (this) {
        DeviceBound.SYSTEM -> MR.strings.sync_bound_system
        DeviceBound.INSTALLED -> MR.strings.sync_bound_installed
        DeviceBound.SECURITY -> MR.strings.sync_bound_security
        DeviceBound.STORAGE -> MR.strings.sync_bound_storage
        DeviceBound.NETWORK -> MR.strings.sync_bound_network
        DeviceBound.NOTIFICATIONS -> MR.strings.sync_bound_notifications
        DeviceBound.HARDWARE -> MR.strings.sync_bound_hardware
        DeviceBound.DIAGNOSTICS -> MR.strings.sync_bound_diagnostics
        DeviceBound.DOWNLOADS -> MR.strings.sync_bound_downloads
        DeviceBound.SYNC -> MR.strings.sync_bound_sync
        DeviceBound.APP -> MR.strings.sync_bound_app
    },
)
