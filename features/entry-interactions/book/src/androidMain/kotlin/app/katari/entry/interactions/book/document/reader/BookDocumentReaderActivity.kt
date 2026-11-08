package app.katari.entry.interactions.book.document.reader

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.core.metro.metroGraph
import app.katari.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import app.katari.entry.interactions.book.processor.BookReaderRequest
import app.katari.entry.interactions.book.reader.BookReaderGraph
import app.katari.entry.interactions.runtime.EntryInteractionActivity
import app.katari.entry.interactions.runtime.EntryInteractionActivityGraph
import app.katari.entry.interactions.runtime.registerEntryInteractionSecureScreen
import app.katari.entry.interactions.runtime.setEntryInteractionContent
import app.katari.entry.interactions.source.EntryChildWebViewAction
import app.katari.entry.interactions.source.launchEntryChildWebViewAction
import app.katari.entry.viewer.settings.navigation.openViewerSettings
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Android's host of the book reader ([BookDocumentReaderController]): the window's system bars, keeping the screen on,
 * the secure screen, and keeping the selection toolbar clear of the translation popup.
 */
internal class BookDocumentReaderActivity : EntryInteractionActivity() {
    private val retainedSessions by viewModels<BookDocumentReaderSessionViewModel>()
    private val selectionActionModeAvoidance = BookSelectionActionModeAvoidance()
    private lateinit var controller: BookDocumentReaderController
    private lateinit var readerSystemBars: BookDocumentReaderSystemBars

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        readerSystemBars = BookDocumentReaderSystemBars(window)
        val activityGraph = metroGraph<EntryInteractionActivityGraph>()
        registerEntryInteractionSecureScreen(activityGraph.readerBasePreferences, activityGraph.securityPreferences)
        val dependencies = metroGraph<BookReaderGraph>().bookReaderDependencies
        val host = BookDocumentReaderHost(
            context = this,
            webViewActions = EntryChildWebViewAction.entries,
            showMessage = { message -> toast(message) },
            onClose = ::finish,
            onChildWebViewAction = { action, resolution, title ->
                launchEntryChildWebViewAction(action, resolution, title)
                    .onFailure { error -> logcat(LogPriority.ERROR, error) { "Failed to launch BOOK source action" } }
            },
            onOpenDefaultSettings = { openViewerSettings(BookDocumentReaderSettingsProvider.PROVIDER_ID) },
            onReopen = ::recreate,
            onTranslationPopupBoundsChanged = selectionActionModeAvoidance::updateBounds,
        )
        controller = BookDocumentReaderController(
            request = BookReaderRequest(
                entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
                chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
            ),
            sessionToken = intent.getStringExtra(EXTRA_SESSION_TOKEN),
            retainedSessions = retainedSessions,
            dependencies = dependencies,
            host = host,
            scope = lifecycleScope,
        )
        val readerContent = ComposeView(this)
        setContentView(
            BookSelectionActionModeHost(this, selectionActionModeAvoidance).apply {
                addView(
                    readerContent,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
        )
        readerContent.setEntryInteractionContent {
            // Back returns to where a link was followed from first, as the desktop's mouse back and Alt+← do. Composed
            // first, so whatever is open over the book closes before it.
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = controller.returnsFromJump,
                onBackCompleted = controller::returnToJumpOrigin,
            )
            BookDocumentReaderContent(controller, host, dependencies)
        }
        lifecycleScope.launch {
            snapshotFlow { controller.systemBars }.collect(readerSystemBars::apply)
        }
        lifecycleScope.launch {
            snapshotFlow { controller.keepsScreenAwake }.collect(::setKeepScreenOn)
        }
        controller.start()
    }

    override fun onStart() {
        super.onStart()
        controller.onStart()
    }

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }

    override fun onStop() {
        controller.onStop(persist = !isChangingConfigurations)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) controller.systemBars?.let(readerSystemBars::apply)
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        controller.readerState?.childWebView?.url?.let { outContent.webUri = Uri.parse(it) }
    }

    override fun onDestroy() {
        selectionActionModeAvoidance.clear()
        controller.close()
        super.onDestroy()
    }

    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"
        private const val EXTRA_SESSION_TOKEN = "session_token"
        fun newIntent(
            context: Context,
            request: BookReaderRequest,
            sessionToken: String,
        ): Intent = Intent(context, BookDocumentReaderActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, request.entryId)
            putExtra(EXTRA_CHAPTER_ID, request.chapterId)
            putExtra(EXTRA_SESSION_TOKEN, sessionToken)
        }
    }
}
