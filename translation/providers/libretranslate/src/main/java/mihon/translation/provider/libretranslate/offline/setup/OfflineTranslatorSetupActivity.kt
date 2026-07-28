package mihon.translation.provider.libretranslate.offline.setup

import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import mihon.translation.provider.libretranslate.R
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorApplication
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorNetwork
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient

internal class OfflineTranslatorSetupActivity : AppCompatActivity() {
    private lateinit var settings: OfflineTranslatorConfiguration
    private lateinit var providerApplication: OfflineTranslatorApplication
    private lateinit var portInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = OfflineTranslatorConfiguration(applicationContext)
        providerApplication = OfflineTranslatorApplication(applicationContext)
        title = getString(R.string.offline_translator_setup_title)
        setContentView(setupContent())
    }

    private fun setupContent(): ScrollView {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        container.addView(
            TextView(this).apply {
                text = getString(R.string.offline_translator_setup_title)
                textSize = 24f
            },
            matchWidth(),
        )
        container.addView(
            TextView(this).apply {
                text = getString(R.string.offline_translator_setup_description)
                setPadding(0, dp(16), 0, dp(16))
            },
            matchWidth(),
        )
        portInput = EditText(this).apply {
            hint = getString(R.string.offline_translator_port_label)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(settings.port.toString())
            setSelectAllOnFocus(true)
        }
        container.addView(portInput, matchWidth())
        container.addView(
            Button(this).apply {
                text = getString(R.string.offline_translator_save_and_test)
                setOnClickListener { saveAndTest() }
            },
            matchWidth(topMargin = 12),
        )
        container.addView(
            Button(this).apply {
                text = getString(R.string.offline_translator_open_app)
                setOnClickListener { providerApplication.open() }
            },
            matchWidth(topMargin = 8),
        )
        status = TextView(this).apply {
            setPadding(0, dp(16), 0, 0)
        }
        container.addView(status, matchWidth())
        return ScrollView(this).apply {
            addView(container)
        }
    }

    private fun saveAndTest() {
        val port = OfflineTranslatorConfiguration.parsePort(portInput.text.toString())
        if (port == null) {
            portInput.error = getString(R.string.offline_translator_invalid_port)
            return
        }
        portInput.error = null
        settings.port = port
        status.setText(R.string.offline_translator_testing)
        lifecycleScope.launch {
            val ready = try {
                LibreTranslateHttpClient(
                    httpClient = OfflineTranslatorNetwork.httpClient,
                    endpoint = settings.endpoint(),
                ).languages().isNotEmpty()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
            status.setText(
                if (ready) {
                    R.string.offline_translator_ready
                } else {
                    R.string.offline_translator_connection_failed
                },
            )
        }
    }

    private fun matchWidth(topMargin: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            this.topMargin = dp(topMargin)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
