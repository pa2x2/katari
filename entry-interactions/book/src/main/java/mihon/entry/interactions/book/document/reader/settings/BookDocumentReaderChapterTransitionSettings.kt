package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Chapter-transition display mode of the BOOK reader. Scoped like its neighbouring reader
 * settings: the profile value is the default and an open entry can override it.
 */
@Composable
internal fun BookDocumentReaderChapterTransitionSettings(
    binding: ViewerSettingBinding<ChapterTransitionMode>,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    ChipsRow(title = stringResource(MR.strings.pref_chapter_transition)) {
        ChapterTransitionMode.entries.forEach { candidate ->
            FilterChip(
                label = stringResource(candidate.titleRes),
                selected = setting.effectiveValue == candidate,
                onClick = {
                    scope.launch {
                        if (candidate == profileDefault) {
                            binding.clearEntryOverride()
                        } else {
                            binding.setEntryOverride(candidate)
                        }
                    }
                },
            )
        }
    }
}
