package eu.kanade.tachiyomi.data.backup.conversion.mihon

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.OtherAppSettingLabel
import tachiyomi.i18n.*

/**
 * Settings only a fork of Mihon or Aniyomi has, which Katari has nothing for, by key with the fork's names for them
 * (D118). Their keys reach the Mihon settings either way, as Aniyomi passes on the keys it doesn't know.
 */
internal object ForkSettings {
    val NO_COUNTERPART: Map<String, OtherAppSettingLabel> = mapOf(
        // TachiyomiSY's reader actions (ReaderPreferences.kt, i18n-sy strings.xml; jobobby04/TachiyomiSY 5c77cad3039d)
        "eh_util_autoscroll_interval" to reader(MR.strings.backup_conversion_sy_autoscroll),
        "eh_util_autoscroll_gestures" to reader(MR.strings.backup_conversion_sy_autoscroll_gestures),
        "eh_util_autoscroll_gesture_toasts" to reader(MR.strings.backup_conversion_sy_autoscroll_gesture_toasts),
        // Anikku's Discord settings (ConnectionsPreferences.kt, SettingsDiscordScreen.kt; komikku-app/anikku 6e35b41f)
        "pref_enable_discord_rpc" to discord(MR.strings.backup_conversion_anikku_discord_rpc),
        "pref_discord_rpc_status" to discord(MR.strings.backup_conversion_anikku_discord_status),
        "pref_discord_rpc_incognito" to discord(MR.strings.backup_conversion_anikku_discord_incognito),
        "discord_rpc_incognito_categories" to
            discord(MR.strings.backup_conversion_anikku_discord_incognito_categories),
        "pref_discord_rpc_use_chapter_titles" to discord(MR.strings.backup_conversion_anikku_discord_episode_titles),
        "pref_discord_custom_message" to discord(MR.strings.backup_conversion_anikku_discord_custom_message),
        "pref_discord_show_progress" to discord(MR.strings.backup_conversion_anikku_discord_show_progress),
        "pref_discord_show_timestamp" to discord(MR.strings.backup_conversion_anikku_discord_show_time),
        "pref_discord_show_buttons" to discord(MR.strings.backup_conversion_anikku_discord_show_buttons),
        "pref_discord_show_download_button" to discord(MR.strings.backup_conversion_anikku_discord_download_button),
        "pref_discord_show_discord_button" to discord(MR.strings.backup_conversion_anikku_discord_discord_button),
    )

    private fun reader(label: StringResource) =
        OtherAppSettingLabel(label, listOf(MR.strings.pref_category_reader))

    private fun discord(label: StringResource) = OtherAppSettingLabel(
        label,
        listOf(
            MR.strings.backup_conversion_anikku_place_connections,
            MR.strings.backup_conversion_anikku_place_discord,
        ),
    )
}
