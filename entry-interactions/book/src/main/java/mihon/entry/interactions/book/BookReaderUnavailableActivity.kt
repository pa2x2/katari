package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Dedicated fallback host used until a compatible BOOK processor can own the reader UI. */
internal class BookReaderUnavailableActivity : ComponentActivity() {
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
                        text = getString(R.string.book_reader_unavailable_message)
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
