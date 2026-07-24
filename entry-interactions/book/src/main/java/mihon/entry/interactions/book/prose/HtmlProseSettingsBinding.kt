package mihon.entry.interactions.book.prose

import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.resetSettings

internal class HtmlProseSettingsBinding(
    private val provider: HtmlProseSettingsProvider,
    private val binder: ViewerSettingBinder,
    private val entryId: Long,
) {
    val theme = binder.bind(provider.themeSetting)
    val fontFamily = binder.bind(provider.fontFamilySetting)
    val fontSize = binder.bind(provider.fontSizeSetting)
    val lineHeight = binder.bind(provider.lineHeightSetting)
    val pageMargins = binder.bind(provider.pageMarginsSetting)
    val textAlignment = binder.bind(provider.textAlignmentSetting)
    val layoutMode = binder.bind(provider.layoutModeSetting, entryId)
    val tapNavigation = binder.bind(provider.tapNavigationSetting)
    val showProgress = binder.bind(provider.showProgressSetting)
    val drawUnderCutout = binder.bind(provider.drawUnderCutoutSetting)

    suspend fun resetSettings() {
        binder.resetSettings(provider, entryId)
    }
}
