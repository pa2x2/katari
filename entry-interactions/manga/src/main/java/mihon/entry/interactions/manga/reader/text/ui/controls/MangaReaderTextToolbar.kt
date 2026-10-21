package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextBlocker
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextProgress
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.InkSelection
import mihon.icons.materialsymbols.rounded.Subtitles
import mihon.icons.materialsymbols.rounded.Visibility
import mihon.icons.materialsymbols.rounded.VisibilityOff
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactFailureLabel
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.chip.AssistChip
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.OverlayPanel
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Status and actions of translate mode, shown while it is active.
 *
 * [languages] names the page language and the target, and is null while the page language is unknown: the target alone
 * would read as the answer to the question which language the pages are in. While the pages on screen are [stored]
 * translations, it names the languages they were translated between and the stored translation can be deleted.
 */
@Composable
internal fun MangaReaderTextToolbar(
    progress: MangaReaderTextProgress,
    overlay: Boolean,
    showOriginal: Boolean,
    languages: String?,
    stored: Boolean,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<List<ModelArtifactState>>,
    onDownloadModels: (List<ModelArtifactDescriptor>) -> Unit,
    onDownloadPlatformModels: (MangaReaderTextBlocker.PlatformModelsRequired) -> Unit,
    onChooseLanguage: () -> Unit,
    onOpenLanguages: () -> Unit,
    onChoosePipeline: (LanguageTag) -> Unit,
    onFixTranslationIssue: (MangaPageTranslationIssue) -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleOriginal: () -> Unit,
    onDeleteStoredTranslation: () -> Unit,
    onSelectArea: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayPanel(modifier = modifier.widthIn(max = KatariTheme.sizes.readableWidth)) {
        Column {
            Row(
                modifier = Modifier.padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                if (progress.isWorking) {
                    LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.iconSmall))
                }
                // Once pages are ready the languages say everything the hint would; page translation problems get a row
                // of their own below.
                val showsProgress = progress != MangaReaderTextProgress.Ready &&
                    progress !is MangaReaderTextProgress.TranslationUnavailable
                if (showsProgress) {
                    TranslationDirectionText(
                        text = progressText(progress, observeModels),
                        role = TextRole.Body,
                        modifier = Modifier.weight(1f, fill = false).padding(vertical = KatariTheme.spacing.m),
                    )
                }
                if (languages != null) {
                    AssistChip(
                        onClick = onOpenLanguages,
                        modifier = Modifier.weight(1f, fill = false),
                        trailingIcon = MaterialSymbols.Rounded.ArrowDropDown,
                    ) {
                        TranslationDirectionText(languages, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                when (val blocker = (progress as? MangaReaderTextProgress.Blocked)?.blocker) {
                    is MangaReaderTextBlocker.ModelsRequired -> Button(
                        label = stringResource(MR.strings.action_download),
                        onClick = { onDownloadModels(blocker.models) },
                        style = ButtonStyle.Text,
                    )
                    is MangaReaderTextBlocker.PlatformModelsRequired -> if (!blocker.installing) {
                        Button(
                            label = stringResource(MR.strings.action_download),
                            onClick = { onDownloadPlatformModels(blocker) },
                            style = ButtonStyle.Text,
                        )
                    }
                    is MangaReaderTextBlocker.PipelineChoiceRequired -> Button(
                        label = stringResource(MR.strings.reader_text_choose_pipeline),
                        onClick = { onChoosePipeline(blocker.language) },
                        style = ButtonStyle.Text,
                    )
                    is MangaReaderTextBlocker.LanguageRequired -> Button(
                        label = stringResource(MR.strings.reader_text_choose_language),
                        onClick = onChooseLanguage,
                        style = ButtonStyle.Text,
                    )
                    else -> IconButton(
                        icon = MaterialSymbols.Rounded.InkSelection,
                        contentDescription = stringResource(MR.strings.reader_text_select_area),
                        onClick = onSelectArea,
                    )
                }
                IconButton(
                    icon = MaterialSymbols.Rounded.Subtitles,
                    contentDescription = stringResource(MR.strings.pref_page_text_translation_overlay),
                    onClick = onToggleOverlay,
                    tone = if (overlay) ContentTone.Primary else ContentTone.Inherit,
                )
                if (overlay) {
                    IconButton(
                        icon = if (showOriginal) {
                            MaterialSymbols.Rounded.Visibility
                        } else {
                            MaterialSymbols.Rounded.VisibilityOff
                        },
                        contentDescription = stringResource(
                            if (showOriginal) {
                                MR.strings.reader_text_show_translations
                            } else {
                                MR.strings.reader_text_show_original
                            },
                        ),
                        onClick = onToggleOriginal,
                    )
                }
                if (stored) {
                    OverflowMenu { close ->
                        DropdownMenuItem(
                            label = stringResource(MR.strings.action_delete_translation),
                            onClick = {
                                close()
                                onDeleteStoredTranslation()
                            },
                        )
                    }
                }
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_close),
                    onClick = onClose,
                )
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
