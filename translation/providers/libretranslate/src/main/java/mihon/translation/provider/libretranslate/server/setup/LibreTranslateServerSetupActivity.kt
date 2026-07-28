package mihon.translation.provider.libretranslate.server.setup

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
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.provider.libretranslate.server.LibreTranslateServerConfiguration
import mihon.translation.provider.libretranslate.server.LibreTranslateServerNetwork

internal class LibreTranslateServerSetupActivity : AppCompatActivity() {
    private lateinit var configuration: LibreTranslateServerConfiguration
    private lateinit var endpointInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration = LibreTranslateServerConfiguration(applicationContext)
        setContentView(content())
    }

    private fun content(): ScrollView {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        fun text(value: String) = TextView(this).apply { this.text = value }
        container.addView(
            text(getString(R.string.libretranslate_server_setup_title)).apply { textSize = 24f },
            matchWidth(),
        )
        container.addView(
            text(getString(R.string.libretranslate_server_setup_description)).apply {
                setPadding(0, dp(16), 0, dp(16))
            },
            matchWidth(),
        )
        endpointInput = EditText(this).apply {
            hint = getString(R.string.libretranslate_server_endpoint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(configuration.endpoint?.toString().orEmpty())
        }
        apiKeyInput = EditText(this).apply {
            hint = getString(R.string.libretranslate_server_api_key)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(configuration.apiKey.orEmpty())
        }
        container.addView(endpointInput, matchWidth())
        container.addView(apiKeyInput, matchWidth(topMargin = 8))
        container.addView(
            Button(this).apply {
                text = getString(R.string.libretranslate_server_save_and_test)
                setOnClickListener { saveAndTest() }
            },
            matchWidth(topMargin = 12),
        )
        status = TextView(this).apply { setPadding(0, dp(16), 0, 0) }
        container.addView(status, matchWidth())
        return ScrollView(this).apply { addView(container) }
    }

    private fun saveAndTest() {
        val endpoint = LibreTranslateServerConfiguration.validateEndpoint(endpointInput.text.toString())
        if (endpoint == null) {
            endpointInput.error = getString(R.string.libretranslate_server_invalid_endpoint)
            return
        }
        endpointInput.error = null
        val apiKey = apiKeyInput.text.toString().trim().takeIf(String::isNotEmpty)
        status.setText(R.string.libretranslate_server_testing)
        lifecycleScope.launch {
            val ready = try {
                LibreTranslateHttpClient(
                    httpClient = LibreTranslateServerNetwork.httpClient,
                    endpoint = endpoint,
                    apiKey = apiKey,
                ).languages().isNotEmpty()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
            val saved = runCatching {
                configuration.save(endpoint, apiKey, verified = ready)
            }.isSuccess
            status.setText(
                if (!saved) {
                    R.string.libretranslate_server_save_failed
                } else if (ready) {
                    R.string.libretranslate_server_ready
                } else {
                    R.string.libretranslate_server_connection_failed
                },
            )
        }
    }

    private fun matchWidth(topMargin: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { this.topMargin = dp(topMargin) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
