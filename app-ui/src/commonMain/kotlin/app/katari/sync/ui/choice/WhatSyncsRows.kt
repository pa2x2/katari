package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CollectionsBookmark
import app.katari.icons.materialsymbols.rounded.Download
import app.katari.icons.materialsymbols.rounded.Lock
import app.katari.icons.materialsymbols.rounded.Storage
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.surface.AvatarSize
import app.katari.presentation.kit.theme.ContentTone
import app.katari.profiles.ui.components.ProfileAvatar
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.part.ProfileSync
import app.katari.sync.client.part.ProfileSyncState
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.setting.scope.SettingsArea
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.core.common.preference.Preference as PreferenceData

/** What the rows of What syncs do. */
internal class WhatSyncsActions(
    val syncData: (DataGroup, Boolean) -> Unit,
    val syncArea: (SettingsArea, Boolean) -> Unit,
    val openArea: (SettingsArea) -> Unit,
    val syncProfile: (ProfileSync, Boolean) -> Unit,
    val syncSetting: (key: String, title: String, on: Boolean) -> Unit,
    val syncMenu: (MenuSettings, Boolean) -> Unit,
)

/**
 * What syncs: the settings and menus this device chose one by one ([chosenApartGroup]), then its choices of data,
 * Settings areas and profiles, then what never syncs. Library and reading progress lead the data, always synced, since
 * every other record belongs to a title.
 */
@Composable
internal fun whatSyncsPreferences(
    choices: ChoiceSet,
    chosenApart: Preference.PreferenceGroup?,
    profiles: List<ProfileSync>,
    askBeforeNewProfiles: PreferenceData<Boolean>,
    actions: WhatSyncsActions,
): List<Preference> = listOfNotNull(
    Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_choices_note)),
    chosenApart,
    dataGroup(choices, actions),
    settingsGroup(choices, actions),
    profilesGroup(profiles, askBeforeNewProfiles, actions),
    neverSyncedGroup(),
)

@Composable
private fun dataGroup(choices: ChoiceSet, actions: WhatSyncsActions): Preference.PreferenceGroup {
    val library = stringResource(MR.strings.sync_choice_library)
    val libraryRow = Preference.PreferenceItem.CustomPreference(title = library) {
        TextPreferenceWidget(
            title = library,
            subtitle = stringResource(MR.strings.sync_choice_library_summary),
            icon = MaterialSymbols.Rounded.CollectionsBookmark,
            widget = { LockedMark() },
        )
    }
    val groupRows = DataGroup.entries.map { group ->
        val title = group.title()
        val summary = group.summary()
        Preference.PreferenceItem.CustomPreference(title = title) {
            SwitchPreferenceWidget(
                title = title,
                subtitle = summary,
                icon = group.icon,
                checked = choices.syncs(group),
                onCheckedChanged = { actions.syncData(group, it) },
            )
        }
    }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.sync_choices_data),
        preferenceItems = listOf(libraryRow) + groupRows,
    )
}

/** Each area's row opens its list of settings, where it has one, and its switch turns the whole area on and off. */
@Composable
private fun settingsGroup(choices: ChoiceSet, actions: WhatSyncsActions) = Preference.PreferenceGroup(
    title = stringResource(MR.strings.sync_choices_settings),
    preferenceItems = SettingsArea.switchable.map { area ->
        val title = area.title()
        val synced = choices.syncs(area)
        val keptHere = choices.settingsOff.count { PreferenceScopes.markOf(it)?.area == area } +
            choices.menusOff.count { PreferenceScopes.areaOf(it) == area }
        val subtitle = when {
            !synced -> stringResource(MR.strings.sync_choice_off)
            keptHere > 0 -> pluralStringResource(MR.plurals.sync_choice_synced_except, keptHere, keptHere)
            else -> stringResource(MR.strings.sync_choice_synced)
        }
        Preference.PreferenceItem.CustomPreference(title = title) {
            SwitchPreferenceWidget(
                title = title,
                subtitle = subtitle,
                icon = area.icon,
                checked = synced,
                onCheckedChanged = { actions.syncArea(area, it) },
                onOpen = if (area.hasPage) {
                    { actions.openArea(area) }
                } else {
                    null
                },
            )
        }
    },
)

/**
 * This device's profiles and the account's that aren't here. The default profile always syncs; an account profile
 * switched off leaves this device, and one made on another device may wait to be added.
 */
@Composable
private fun profilesGroup(
    profiles: List<ProfileSync>,
    askBeforeNewProfiles: PreferenceData<Boolean>,
    actions: WhatSyncsActions,
): Preference.PreferenceGroup {
    val onlyOneHere = profiles.count { it.isHere } == 1
    val profileRows = profiles.map { profile ->
        val subtitle = stringResource(
            when {
                profile.isDefault && onlyOneHere -> MR.strings.sync_profile_only_one
                profile.isDefault -> MR.strings.sync_profile_default
                profile.state == ProfileSyncState.ASKING -> MR.strings.sync_profile_asking
                profile.state == ProfileSyncState.OFF && profile.isHere -> MR.strings.sync_profile_only_here
                profile.state == ProfileSyncState.OFF -> MR.strings.sync_profile_not_here
                else -> MR.strings.sync_choice_synced
            },
        )
        Preference.PreferenceItem.CustomPreference(title = profile.name) {
            SwitchPreferenceWidget(
                title = profile.name,
                subtitle = subtitle,
                leading = { ProfileAvatar(colorSeed = profile.colorSeed, size = AvatarSize.Row) },
                checked = profile.state == ProfileSyncState.SYNCED,
                enabled = !profile.isDefault,
                onCheckedChanged = { actions.syncProfile(profile, it) },
            )
        }
    }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.sync_choices_profiles),
        preferenceItems = profileRows + Preference.PreferenceItem.SwitchPreference(
            preference = askBeforeNewProfiles,
            title = stringResource(MR.strings.sync_ask_new_profiles),
            subtitle = stringResource(MR.strings.sync_ask_new_profiles_summary),
        ),
    )
}

@Composable
private fun neverSyncedGroup(): Preference.PreferenceGroup {
    @Composable
    fun row(
        icon: ImageVector,
        titleRes: StringResource,
        summaryRes: StringResource,
    ): Preference.PreferenceItem.CustomPreference {
        val title = stringResource(titleRes)
        val summary = stringResource(summaryRes)
        return Preference.PreferenceItem.CustomPreference(title = title) {
            TextPreferenceWidget(title = title, subtitle = summary, icon = icon)
        }
    }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.sync_choices_never),
        preferenceItems = listOf(
            row(
                MaterialSymbols.Rounded.Download,
                MR.strings.sync_choice_never_downloads,
                MR.strings.sync_choice_never_downloads_summary,
            ),
            row(
                MaterialSymbols.Rounded.Lock,
                MR.strings.sync_choice_never_logins,
                MR.strings.sync_choice_never_logins_summary,
            ),
            row(
                MaterialSymbols.Rounded.Storage,
                MR.strings.sync_choice_never_device,
                MR.strings.sync_choice_never_device_summary,
            ),
        ),
    )
}

/** The end of a row that can't be switched off. */
@Composable
internal fun LockedMark() {
    Icon(MaterialSymbols.Rounded.Lock, contentDescription = null, tone = ContentTone.Secondary)
}
