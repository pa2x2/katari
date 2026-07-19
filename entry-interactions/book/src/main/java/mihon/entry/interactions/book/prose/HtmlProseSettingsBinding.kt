package mihon.entry.interactions.book.prose

import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder

internal class HtmlProseSettingsBinding(
    provider: HtmlProseSettingsProvider,
    binder: ViewerSettingBinder,
    entryId: Long,
) {
    val theme = binder.bind(provider.themeSetting)
    val fontFamily = binder.bind(provider.fontFamilySetting)
    val fontSize = binder.bind(provider.fontSizeSetting)
    val lineHeight = binder.bind(provider.lineHeightSetting)
    val pageMargins = binder.bind(provider.pageMarginsSetting)
    val paragraphSpacing = binder.bind(provider.paragraphSpacingSetting)
    val textAlignment = binder.bind(provider.textAlignmentSetting)
    val layoutMode = binder.bind(provider.layoutModeSetting, entryId)
    val tapNavigation = binder.bind(provider.tapNavigationSetting)
    val showProgress = binder.bind(provider.showProgressSetting)
    val drawUnderCutout = binder.bind(provider.drawUnderCutoutSetting)
}
