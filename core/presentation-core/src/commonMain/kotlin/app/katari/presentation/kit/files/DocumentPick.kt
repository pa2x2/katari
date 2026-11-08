package app.katari.presentation.kit.files

import tachiyomi.domain.storage.DocumentUri

/**
 * A document the user picked.
 *
 * @property accessKept whether the access the request asked to keep was kept. False when it asked for none, or when the
 * document's provider refused to keep it; the picker's temporary grant still serves for now.
 */
data class DocumentPick(
    val uri: DocumentUri,
    val accessKept: Boolean,
)
