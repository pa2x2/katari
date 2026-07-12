package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.duplicate.DuplicateTitleExclusionsScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object DuplicateDetectionSettingsScreen : SearchableSettings {
    private fun readResolve(): Any = DuplicateDetectionSettingsScreen

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_duplicate_detection

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val duplicatePreferences = remember { Injekt.get<DuplicatePreferences>() }

        val extendedEnabled by duplicatePreferences.extendedDuplicateDetectionEnabled.collectAsState()
        val minimumMatchScore by duplicatePreferences.minimumMatchScore.collectAsState()
        val descriptionWeight by duplicatePreferences.descriptionWeight.collectAsState()
        val authorWeight by duplicatePreferences.authorWeight.collectAsState()
        val artistWeight by duplicatePreferences.artistWeight.collectAsState()
        val coverWeight by duplicatePreferences.coverWeight.collectAsState()
        val genreWeight by duplicatePreferences.genreWeight.collectAsState()
        val statusWeight by duplicatePreferences.statusWeight.collectAsState()
        val chapterCountWeight by duplicatePreferences.chapterCountWeight.collectAsState()
        val titleWeight by duplicatePreferences.titleWeight.collectAsState()
        val titleExclusionPatterns by duplicatePreferences.titleExclusionPatterns.collectAsState()

        val budget = remember(
            descriptionWeight,
            authorWeight,
            artistWeight,
            coverWeight,
            genreWeight,
            statusWeight,
            chapterCountWeight,
            titleWeight,
        ) {
            DuplicatePreferences.DuplicateWeightBudget(
                description = descriptionWeight,
                author = authorWeight,
                artist = artistWeight,
                cover = coverWeight,
                genre = genreWeight,
                status = statusWeight,
                chapterCount = chapterCountWeight,
                title = titleWeight,
            ).normalized()
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_duplicate_detection_behavior),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = duplicatePreferences.extendedDuplicateDetectionEnabled,
                        title = stringResource(MR.strings.pref_enable_extended_duplicate_detection),
                        subtitle = stringResource(MR.strings.pref_enable_extended_duplicate_detection_summary),
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        title = stringResource(MR.strings.pref_duplicate_detection_matching_info),
                        showIcon = false,
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        preference = duplicatePreferences.minimumMatchScore,
                        title = stringResource(MR.strings.pref_duplicate_detection_minimum_score),
                        value = minimumMatchScore,
                        valueString = minimumMatchScore.toString(),
                        subtitle = stringResource(MR.strings.pref_duplicate_detection_minimum_score_summary),
                        enabled = extendedEnabled,
                        valueRange = 0..DuplicatePreferences.TOTAL_SCORE_BUDGET,
                        onValueChanged = { duplicatePreferences.minimumMatchScore.set(it) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_duplicate_detection_reset_settings),
                        subtitle = stringResource(MR.strings.pref_duplicate_detection_reset_settings_summary),
                        enabled = extendedEnabled,
                        isProfileSpecific = true,
                        onClick = duplicatePreferences::resetDetectionSettings,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_duplicate_detection_title_exclusions),
                        subtitle = pluralStringResource(
                            MR.plurals.pref_duplicate_detection_title_exclusions_count,
                            titleExclusionPatterns.size,
                            titleExclusionPatterns.size,
                        ),
                        enabled = extendedEnabled,
                        isProfileSpecific = true,
                        onClick = {
                            navigator.push(DuplicateTitleExclusionsScreen())
                        },
                    ),
                ),
            ),
        ) + buildList {
            if (extendedEnabled) {
                add(
                    Preference.PreferenceGroup(
                        title = stringResource(MR.strings.pref_duplicate_detection_confidence_budget),
                        preferenceItems = persistentListOf(
                            Preference.PreferenceItem.InfoPreference(
                                title = stringResource(
                                    MR.strings.pref_duplicate_detection_unused_budget,
                                    budget.remainingBudget,
                                    DuplicatePreferences.TOTAL_SCORE_BUDGET,
                                ),
                                showIcon = false,
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.descriptionWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_description),
                                value = budget.description,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_description,
                                    budget.description,
                                    budget.description + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.description + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.descriptionWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.authorWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_author),
                                value = budget.author,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_author,
                                    budget.author,
                                    budget.author + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.author + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.authorWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.artistWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_artist),
                                value = budget.artist,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_artist,
                                    budget.artist,
                                    budget.artist + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.artist + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.artistWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.coverWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_cover),
                                value = budget.cover,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_cover,
                                    budget.cover,
                                    budget.cover + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.cover + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.coverWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.genreWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_genre),
                                value = budget.genre,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_genre,
                                    budget.genre,
                                    budget.genre + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.genre + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.genreWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.statusWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_status),
                                value = budget.status,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_status,
                                    budget.status,
                                    budget.status + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.status + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.statusWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.chapterCountWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_chapter_count),
                                value = budget.chapterCount,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_chapter_count,
                                    budget.chapterCount,
                                    budget.chapterCount + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.chapterCount + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.chapterCountWeight.set(it) },
                            ),
                            Preference.PreferenceItem.SliderPreference(
                                preference = duplicatePreferences.titleWeight,
                                title = stringResource(MR.strings.pref_duplicate_detection_weight_title),
                                value = budget.title,
                                subtitle = stringResource(
                                    MR.strings.pref_duplicate_detection_weight_summary_title,
                                    budget.title,
                                    budget.title + budget.remainingBudget,
                                ),
                                enabled = true,
                                valueRange = 0..(budget.title + budget.remainingBudget),
                                onValueChanged = { duplicatePreferences.titleWeight.set(it) },
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
