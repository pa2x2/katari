package app.katari.sync.client.firstsync

import app.katari.domain.profile.ProfileSeedPreferences
import app.katari.sync.client.RecordExchange
import app.katari.sync.client.SyncTestDevice
import app.katari.sync.client.library
import eu.kanade.domain.base.OnboardingPreferences
import eu.kanade.domain.ui.UiPreferences
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FirstSyncTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `light or dark picked in the unfinished first-run guide isn't the device's own data`(): Unit = runBlocking {
        SyncTestDevice(directory.resolve("b.db")).use { b ->
            ProfileSeedPreferences.seed(b.preferences.profiles.profileStore(1))
            b.preferences.profiles.profileStore(1).getString(UiPreferences.THEME_MODE_KEY).set("DARK")
            b.firstSync.hasOwnData() shouldBe false

            OnboardingPreferences(b.preferences.profiles.basePreferenceStore()).shownOnboardingFlow.set(true)
            b.firstSync.hasOwnData() shouldBe true
        }
    }

    @Test
    fun `a device with data joins its profile to the account profile of its name, keeping its settings`(): Unit =
        runBlocking {
            val exchange = RecordExchange()
            SyncTestDevice(directory.resolve("a.db")).use { a ->
                SyncTestDevice(directory.resolve("b.db")).use { b ->
                    // What the app writes on its first start, before the person does anything
                    ProfileSeedPreferences.seed(b.preferences.profiles.profileStore(1))
                    b.firstSync.hasOwnData() shouldBe false
                    b.sql(
                        "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'b-work', 'work', 1, 1)",
                    )
                    b.preferences.profiles.profileStore(2).getString(DISPLAY_MODE).set("grid")
                    b.firstSync.hasOwnData() shouldBe true

                    a.startSync(deviceId = 1)
                    a.sql(
                        "INSERT INTO profiles(_id, uuid, name, color_seed, position) VALUES (2, 'a-work', 'Work', 1, 1)",
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(profile_id, source, url, title, favorite, type)
                        VALUES (2, -2, '/a-show', 'Show', 1, 'anime')
                        """,
                    )
                    a.preferences.profiles.profileStore(2).getString(DISPLAY_MODE).set("list")
                    val account = exchange.pushFrom(a)
                    delay(5)

                    b.startSync(deviceId = 2)
                    val proposal = b.firstSync.proposal(account.map { it.record })
                    proposal.single { it.profileId == 2L }.pairedWith shouldBe AccountProfile("a-work", "Work")
                    b.firstSync.pair(
                        proposal.map {
                            ProfileChoice(it.profileId, it.pairedWith?.uuid, SettingsWinner.DEVICE)
                        },
                    )
                    // What unit 8 records of the device's rows before applying the account's
                    b.sql(
                        "INSERT INTO source_identities(local_key, id_scheme, external_id) VALUES (-2, 'aniyomi', 777)",
                        """
                        INSERT INTO entries(profile_id, source, url, title, favorite, type)
                        VALUES (2, -2, '/b-show', 'Show', 1, 'anime')
                        """,
                    )
                    exchange.deliver(b, account)
                    exchange.settle(listOf(a, b))

                    a.library() shouldBe b.library()
                    a.library() shouldContainAll listOf(
                        """["profile","a-work","Work",1,1,0]""",
                        """["entry","a-work","aniyomi",777,"/a-show","anime",1,0,null,"",0,0,0]""",
                        """["entry","a-work","aniyomi",777,"/b-show","anime",1,0,null,"",0,0,0]""",
                    )
                    a.preferences.profiles.profileStore(2).getString(DISPLAY_MODE).get() shouldBe "grid"
                    b.preferences.profiles.profileStore(2).getString(DISPLAY_MODE).get() shouldBe "grid"
                }
            }
        }

    private companion object {
        const val DISPLAY_MODE = "pref_display_mode_library"
    }
}
