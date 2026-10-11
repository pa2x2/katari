package app.katari.showcase.capture.library

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.capture
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.restart
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.openProfile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScenes {
    @Test
    fun profiles() {
        restart()
        find(By.text("Select profile"))
        capture("profiles")
    }

    @Test
    fun library() {
        openProfile("Personal")
        capture("library")
    }

    @Test
    fun mergedEntry() {
        openProfile("Personal")
        tap(By.text("Pepper&Carrot"))
        find(By.textContains("ペッパー"))
        capture("merged-entry")
    }

    @Test
    fun updates() {
        openProfile("Family")
        tap(By.text("Updates"))
        find(By.textContains("Episode 7"))
        capture("updates")
    }
}
