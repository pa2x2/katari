package eu.kanade.presentation.library.update

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.i18n.*

val EntryUpdateDecisionReason.labelRes: StringResource
    get() = when (this) {
        EntryUpdateDecisionReason.ENTRY_NEVER -> MR.strings.library_update_reason_never
        EntryUpdateDecisionReason.CATEGORY_OFF -> MR.strings.library_update_reason_category_off
        EntryUpdateDecisionReason.SOURCE_OFF -> MR.strings.library_update_reason_source_off
        EntryUpdateDecisionReason.TYPE_OFF -> MR.strings.library_update_reason_type_off
        EntryUpdateDecisionReason.FETCH_ONCE -> MR.strings.library_update_reason_fetch_once
        EntryUpdateDecisionReason.COMPLETED -> MR.strings.library_update_reason_completed
        EntryUpdateDecisionReason.HAS_UNSEEN -> MR.strings.library_update_reason_has_unseen
        EntryUpdateDecisionReason.NOT_STARTED -> MR.strings.library_update_reason_not_started
        EntryUpdateDecisionReason.OUTSIDE_RELEASE_PERIOD -> MR.strings.library_update_reason_outside_release
        EntryUpdateDecisionReason.NOT_DUE -> MR.strings.library_update_reason_not_due
    }
