package eu.kanade.tachiyomi.data.backup.conversion

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Name

/**
 * How the app a backup came from labels one of its settings and the [place] it keeps it, such as Mihon's Privacy
 * settings (D053, D055, D075). One table serves every app of a family, so the report names the app the backup came
 * from.
 */
internal class OtherAppSettingLabel(private val label: StringResource, private val place: List<StringResource>) {
    fun nameIn(app: String): Name = Name.OtherAppSetting(app, place, label)
}
