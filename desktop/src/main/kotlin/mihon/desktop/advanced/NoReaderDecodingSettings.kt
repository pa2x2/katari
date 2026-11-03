package mihon.desktop.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.advanced.ReaderDecodingSettings

/** Desktop has no readers yet, so there is no page decoding to tune. Stands in until desktop hosts the readers. */
@Inject
class NoReaderDecodingSettings : ReaderDecodingSettings {
    override val isOffered: Boolean = false

    @Composable
    override fun preferenceItems(): List<Preference.PreferenceItem<out Any, out Any>> = emptyList()
}
