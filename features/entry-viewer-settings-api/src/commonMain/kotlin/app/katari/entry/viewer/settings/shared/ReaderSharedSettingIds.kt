package app.katari.entry.viewer.settings.shared

@JvmInline
value class ReaderCapabilityId(
    val value: String,
) {
    init {
        require(value.matches(Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*"""))) {
            "Invalid reader capability id: '$value'"
        }
    }
}

object StandardReaderCapabilities {
    val StableTextSelection = ReaderCapabilityId("text-selection.stable")
    val SelectionAnchoring = ReaderCapabilityId("text-selection.anchor")
    val NextChapterPreparation = ReaderCapabilityId("chapter.next-preparation")
}

@JvmInline
value class ReaderSharedSettingId(
    val value: String,
) {
    init {
        require(value.matches(Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*"""))) {
            "Invalid shared reader setting id: '$value'"
        }
    }
}

object StandardReaderSharedSettingIds {
    val NextChapterPreparation = ReaderSharedSettingId("reader.prepare-next-chapter")
}
