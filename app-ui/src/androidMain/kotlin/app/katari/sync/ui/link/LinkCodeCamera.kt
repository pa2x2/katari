package app.katari.sync.ui.link

import android.content.Context
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.awaitCancellation
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.Executors

/**
 * The back camera's picture while [scanning], with every QR code ZXing reads in it handed to [onText] on the main
 * thread. Frames are read one at a time on their own thread, the newest only, so a slow phone skips frames rather than
 * falling behind. [onUnavailable] hears when the device has no camera Katari can open.
 */
@Composable
internal fun LinkCodeCamera(
    scanning: Boolean,
    onText: (String) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnText by rememberUpdatedState(onText)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    LaunchedEffect(scanning) {
        if (!scanning) return@LaunchedEffect
        val bound = CameraBinding.bind(context, lifecycleOwner, { surfaceRequest = it }) { text ->
            context.mainExecutor.execute { currentOnText(text) }
        }
        if (bound == null) {
            currentOnUnavailable()
            return@LaunchedEffect
        }
        try {
            awaitCancellation()
        } finally {
            bound()
        }
    }

    surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = modifier) }
}

private object CameraBinding {
    /** Binds the preview and the reader to [owner]; returns what unbinds them, or null when no camera could be bound. */
    suspend fun bind(
        context: Context,
        owner: LifecycleOwner,
        onSurface: (SurfaceRequest) -> Unit,
        onText: (String) -> Unit,
    ): (() -> Unit)? {
        val provider = try {
            ProcessCameraProvider.awaitInstance(context)
        } catch (e: IllegalStateException) {
            logcat(LogPriority.WARN, e) { "No camera provider" }
            return null
        }
        if (!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) return null
        val preview = Preview.Builder().build().apply { setSurfaceProvider(onSurface) }
        val executor = Executors.newSingleThreadExecutor()
        val reader = QrFrameReader(onText)
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { setAnalyzer(executor, reader) }
        try {
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        } catch (e: IllegalArgumentException) {
            logcat(LogPriority.WARN, e) { "Couldn't bind the camera" }
            executor.shutdown()
            return null
        }
        return {
            provider.unbind(preview, analysis)
            executor.shutdown()
        }
    }
}

/** Reads QR codes from the brightness plane of each frame, which is all ZXing needs. */
private class QrFrameReader(private val onText: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    override fun analyze(image: ImageProxy) {
        image.use {
            val plane = it.planes.first()
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining()).also(buffer::get)
            val rowStride = plane.rowStride
            // The last row may stop at the picture's width rather than the stride.
            val height = minOf(it.height, bytes.size / rowStride)
            val source = PlanarYUVLuminanceSource(bytes, rowStride, height, 0, 0, it.width, height, false)
            try {
                onText(reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text)
            } catch (_: NotFoundException) {
                // No code in this frame
            } catch (_: ReaderException) {
                // A code seen too badly to read; the next frame may do
            } finally {
                reader.reset()
            }
        }
    }
}
