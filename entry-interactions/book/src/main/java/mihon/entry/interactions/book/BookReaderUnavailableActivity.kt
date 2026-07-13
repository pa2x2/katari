package mihon.entry.interactions.book

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mihon.book.api.BookFailure
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Dedicated fallback host used until a compatible BOOK processor can own the reader UI. */
internal class BookReaderUnavailableActivity : ComponentActivity() {
    private lateinit var messageView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.book_reader_unavailable_title)

        val spacing = (resources.displayMetrics.density * 24).toInt()
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(spacing, spacing, spacing, spacing)

                addView(
                    TextView(context).apply {
                        text = getString(R.string.book_reader_unavailable_title)
                        textSize = 24f
                        gravity = Gravity.CENTER
                    },
                    matchWidth(),
                )
                addView(
                    TextView(context).apply {
                        messageView = this
                        text = getString(R.string.book_reader_loading)
                        textSize = 16f
                        gravity = Gravity.CENTER
                    },
                    matchWidth(topMargin = spacing),
                )
                addView(
                    Button(context).apply {
                        text = getString(R.string.book_reader_close)
                        setOnClickListener { finish() }
                    },
                    wrapContent(topMargin = spacing),
                )
            },
        )

        lifecycleScope.launch {
            val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
            val chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L)
            val state = if (entryId < 0L || chapterId < 0L) {
                BookReaderHostState.Unavailable(
                    BookFailure(
                        reason = mihon.book.api.BookFailureReason.CONTENT_UNAVAILABLE,
                        message = "The selected book item is invalid.",
                    ),
                )
            } else {
                Injekt.get<BookReaderHostResolver>().resolve(entryId, chapterId)
            }
            render(state)
        }
    }

    private fun render(state: BookReaderHostState) {
        when (state) {
            is BookReaderHostState.Unavailable -> {
                messageView.text = getString(
                    R.string.book_reader_unavailable_message,
                    state.failure.reason.displayName(),
                    state.failure.message,
                )
            }
            is BookReaderHostState.ChoiceRequired -> showProcessorChooser(state)
            is BookReaderHostState.ReaderSelected -> {
                messageView.text = getString(
                    R.string.book_reader_selected_message,
                    state.processorName,
                )
            }
        }
    }

    private fun showProcessorChooser(state: BookReaderHostState.ChoiceRequired) {
        var selectedIndex = 0
        val rememberChoice = CheckBox(this).apply {
            text = getString(R.string.book_reader_remember_choice)
            val spacing = (resources.displayMetrics.density * 16).toInt()
            setPadding(spacing, 0, spacing, 0)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.book_reader_choose_title)
            .setSingleChoiceItems(
                state.choices.map(BookProcessorChoice::displayName).toTypedArray(),
                selectedIndex,
            ) { _, index -> selectedIndex = index }
            .setView(rememberChoice)
            .setNegativeButton(R.string.book_reader_close) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.book_reader_confirm) { _, _ ->
                val choice = state.choices[selectedIndex]
                render(
                    Injekt.get<BookReaderHostResolver>().choose(
                        state = state,
                        processorId = choice.id,
                        remember = rememberChoice.isChecked,
                    ),
                )
            }
            .show()
    }

    private fun matchWidth(topMargin: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { this.topMargin = topMargin }

    private fun wrapContent(topMargin: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        this.topMargin = topMargin
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"

        fun newIntent(context: Context, entry: Entry, chapter: EntryChapter): Intent {
            entry.requireBook()
            return Intent(context, BookReaderUnavailableActivity::class.java).apply {
                putExtra(EXTRA_ENTRY_ID, entry.id)
                putExtra(EXTRA_CHAPTER_ID, chapter.id)
            }
        }
    }
}

private fun mihon.book.api.BookFailureReason.displayName(): String {
    return name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
}
