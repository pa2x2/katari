package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HighlightAlt
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextBlocker
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextProgress
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactFailureLabel
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Status and actions of translate mode, shown while it is active.
 *
 * [languages] names the page language and the target, and is null while the page language is unknown: the target alone
 * would read as the answer to the question which language the pages are in.
 */
@Composable
internal fun MangaReaderTextToolbar(
    progress: MangaReaderTextProgress,
    overlay: Boolean,
    showOriginal: Boolean,
    languages: String?,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<List<ModelArtifactState>>,
    onDownloadModels: (List<ModelArtifactDescriptor>) -> Unit,
    onDownloadPlatformModels: (MangaReaderTextBlocker.PlatformModelsRequired) -> Unit,
    onChooseLanguage: () -> Unit,
    onOpenLanguages: () -> Unit,
    onChoosePipeline: (LanguageTag) -> Unit,
    onFixTranslationIssue: (MangaPageTranslationIssue) -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleOriginal: () -> Unit,
    onSelectArea: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (progress.isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                // Once pages are ready the languages say everything the hint would; page translation problems get a row
                // of their own below.
                val showsProgress = progress != MangaReaderTextProgress.Ready &&
                    progress !is MangaReaderTextProgress.TranslationUnavailable
                if (showsProgress) {
                    TranslationDirectionText(
                        text = progressText(progress, observeModels),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false).padding(vertical = 12.dp),
                    )
                }
                if (languages != null) {
                    AssistChip(
                        onClick = onOpenLanguages,
                        label = { TranslationDirectionText(languages, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f, fill = false),
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                    )
                }
                when (val blocker = (progress as? MangaReaderTextProgress.Blocked)?.blocker) {
                    is MangaReaderTextBlocker.ModelsRequired -> TextButton(
                        onClick = { onDownloadModels(blocker.models) },
                    ) {
                        Text(stringResource(MR.strings.action_download))
                    }
                    is MangaReaderTextBlocker.PlatformModelsRequired -> if (!blocker.installing) {
                        TextButton(onClick = { onDownloadPlatformModels(blocker) }) {
                            Text(stringResource(MR.strings.action_download))
                        }
                    }
                    is MangaReaderTextBlocker.PipelineChoiceRequired -> TextButton(
                        onClick = { onChoosePipeline(blocker.language) },
                    ) {
                        Text(stringResource(MR.strings.reader_text_choose_pipeline))
                    }
                    is MangaReaderTextBlocker.LanguageRequired -> TextButton(onClick = onChooseLanguage) {
                        Text(stringResource(MR.strings.reader_text_choose_language))
                    }
                    else -> IconButton(onClick = onSelectArea) {
                        Icon(
                            Icons.Outlined.HighlightAlt,
                            contentDescription = stringResource(MR.strings.reader_text_select_area),
                        )
                    }
                }
                IconButton(onClick = onToggleOverlay) {
                    Icon(
                        imageVector = Icons.Outlined.Subtitles,
                        contentDescription = stringResource(MR.strings.pref_page_text_translation_overlay),
                        tint = if (overlay) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                }
                if (overlay) {
                    IconButton(onClick = onToggleOriginal) {
                        Icon(
                            imageVector = if (showOriginal) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = stringResource(
                                if (showOriginal) {
                                    MR.strings.reader_text_show_translations
                                } else {
                                    MR.strings.reader_text_show_original
                                },
                            ),
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(MR.strings.action_close))
                }
            }
            (progress as? MangaReaderTextProgress.TranslationUnavailable)?.let {
                MangaPageTranslationIssueRow(issue = it.issue, onFix = onFixTranslationIssue)
            }
        }
    }
}

@Composable
private fun progressText(
    progress: MangaReaderTextProgress,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<List<ModelArtifactState>>,
): String = when (progress) {
    MangaReaderTextProgress.Waiting -> stringResource(MR.strings.reader_text_waiting)
    MangaReaderTextProgress.Recognizing -> stringResource(MR.strings.reader_text_recognizing)
    MangaReaderTextProgress.Translating -> stringResource(MR.strings.reader_text_translating)
    is MangaReaderTextProgress.TranslationUnavailable -> mangaPageTranslationIssueMessage(progress.issue)
    MangaReaderTextProgress.Ready -> stringResource(MR.strings.reader_text_ready)
    MangaReaderTextProgress.NoText -> stringResource(MR.strings.reader_text_no_text)
    is MangaReaderTextProgress.Failed -> stringResource(MR.strings.reader_text_failed, progress.message.orEmpty())
    is MangaReaderTextProgress.Blocked -> when (val blocker = progress.blocker) {
        is MangaReaderTextBlocker.ModelsRequired -> modelsText(blocker.models, observeModels)
        is MangaReaderTextBlocker.PlatformModelsRequired -> if (blocker.installing) {
            stringResource(MR.strings.reader_text_platform_models_installing)
        } else {
            blocker.description
        }
        is MangaReaderTextBlocker.PipelineChoiceRequired ->
            stringResource(MR.strings.reader_text_pipeline_required, blocker.language.displayName())
        is MangaReaderTextBlocker.LanguageRequired -> stringResource(MR.strings.reader_text_language_required)
        is MangaReaderTextBlocker.UnsupportedLanguage ->
            stringResource(MR.strings.reader_text_unsupported_language, blocker.language.displayName())
        is MangaReaderTextBlocker.Unavailable -> stringResource(MR.strings.reader_text_unavailable, blocker.reason)
    }
}

/** Download progress once approved models are downloading, otherwise what they would take. */
@Composable
private fun modelsText(
    models: List<ModelArtifactDescriptor>,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<List<ModelArtifactState>>,
): String {
    val states by remember(models) { observeModels(models) }.collectAsState(initial = emptyList())
    states.filterIsInstance<ModelArtifactState.Failed>().firstOrNull()?.let {
        return modelArtifactFailureLabel(it.failure)
    }
    val downloading = states.filterIsInstance<ModelArtifactState.Downloading>()
    if (downloading.isNotEmpty()) {
        val total = models.sumOf { it.sizeBytes }
        val done = states.zip(models).sumOf { (state, model) ->
            when (state) {
                is ModelArtifactState.Downloading -> state.downloadedBytes
                is ModelArtifactState.Installed -> model.sizeBytes
                else -> 0L
            }
        }
        return stringResource(
            MR.strings.reader_text_models_downloading,
            formatModelArtifactSize(done),
            formatModelArtifactSize(total),
        )
    }
    return stringResource(
        MR.strings.reader_text_models_required,
        formatModelArtifactSize(
            models.sumOf {
                it.sizeBytes
            },
        ),
    )
}
