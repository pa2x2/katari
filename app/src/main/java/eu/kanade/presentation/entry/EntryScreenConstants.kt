package eu.kanade.presentation.entry

enum class DownloadAction {
    NEXT_1_CHAPTER,
    NEXT_5_CHAPTERS,
    NEXT_10_CHAPTERS,
    NEXT_25_CHAPTERS,
    UNREAD_CHAPTERS,
    BOOKMARKED_CHAPTERS,
}

enum class EditCoverAction {
    EDIT,
    DELETE,
}

enum class EntryScreenItem {
    INFO_BOX,
    MERGE_NOTICE,
    ACTION_ROW,
    DESCRIPTION_WITH_TAG,
    CHAPTER_HEADER,
    CHAPTER_GROUP_HEADER,
    CHAPTER,
}
