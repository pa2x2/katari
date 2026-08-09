package mihon.entry.interactions.book.document.reader

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import mihon.entry.interactions.book.R

internal class BookDocumentSelectionActionMode(
    private val view: BookDocumentTextView,
) : ActionMode.Callback2() {
    private var activeMode: ActionMode? = null

    // Floating toolbars snapshot visible entries, so swapping stable items refreshes reliably across Android versions.
    private var listenItem: MenuItem? = null
    private var stopItem: MenuItem? = null
    private var translateItem: MenuItem? = null

    fun invalidate() {
        activeMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        activeMode = mode
        listenItem = menu.add(
            Menu.NONE,
            R.id.book_reader_selection_action_listen,
            SELECTION_ACTION_ORDER,
            "",
        ).apply { setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM) }
        stopItem = menu.add(
            Menu.NONE,
            R.id.book_reader_selection_action_stop,
            SELECTION_ACTION_ORDER,
            view.context.getString(R.string.book_reader_selection_stop_listening),
        ).apply {
            isVisible = false
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        translateItem = menu.add(
            Menu.NONE,
            R.id.book_reader_selection_action_translate,
            SELECTION_ACTION_ORDER + 1,
            "",
        ).apply { setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM) }
        updateActions(menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        activeMode = mode
        updateActions(menu)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val action = item.bookSelectionAction ?: return false
        view.dispatchSelectionAction(action)
        mode.invalidate()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        if (activeMode === mode) {
            activeMode = null
            listenItem = null
            stopItem = null
            translateItem = null
        }
    }

    private fun updateActions(menu: Menu) {
        val interaction = view.selectionInteraction
        val actions = interaction?.selectionActions.orEmpty()
        val existingTitles = menu.nonKatariItemTitles()
        val activeSpeech = interaction?.activeSpeechSelectionIdentity == view.currentSelectionIdentity()
        val listenEnabled = interaction?.observeSelections == true &&
            BookDocumentSelectionAction.Listen in actions
        listenItem?.isVisible = listenEnabled && !activeSpeech
        stopItem?.isVisible = listenEnabled && activeSpeech
        if (listenEnabled) {
            listenItem?.title = when {
                existingTitles.containsTitle(view.context.getString(R.string.book_reader_selection_listen)) ->
                    view.context.getString(R.string.book_reader_selection_listen_with_katari)
                else -> view.context.getString(R.string.book_reader_selection_listen)
            }
        }

        val translateEnabled = interaction?.observeSelections == true &&
            BookDocumentSelectionAction.Translate in actions
        translateItem?.isVisible = translateEnabled
        if (translateEnabled) {
            val baseTitle = view.context.getString(R.string.book_reader_selection_translate)
            translateItem?.title = if (existingTitles.containsTitle(baseTitle)) {
                view.context.getString(R.string.book_reader_selection_translate_with_katari)
            } else {
                baseTitle
            }
        }
    }
}

private val MenuItem.bookSelectionAction: BookDocumentSelectionAction?
    get() = when (itemId) {
        R.id.book_reader_selection_action_listen -> BookDocumentSelectionAction.Listen
        R.id.book_reader_selection_action_stop -> BookDocumentSelectionAction.Listen
        R.id.book_reader_selection_action_translate -> BookDocumentSelectionAction.Translate
        else -> null
    }

private fun Menu.nonKatariItemTitles(): List<CharSequence> {
    val titles = ArrayList<CharSequence>(size())
    repeat(size()) { index ->
        val item = getItem(index)
        if (item.bookSelectionAction == null) item.title?.let(titles::add)
    }
    return titles
}

private fun List<CharSequence>.containsTitle(candidate: String): Boolean = any {
    it.toString().trim().equals(candidate.trim(), ignoreCase = true)
}

private const val SELECTION_ACTION_ORDER = Menu.CATEGORY_SECONDARY + 10
