package com.qrdatasender.app.ui

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ScannerPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    showPreview: Boolean,
    onCode: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val latestOnCode by rememberUpdatedState(onCode)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestShowPreview by rememberUpdatedState(showPreview)
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val preview = remember(previewView) {
        Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val disposed = AtomicBoolean(false)
        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (!latestEnabled || mediaImage == null) {
                imageProxy.close()
            } else {
                val image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { results ->
                        if (!disposed.get()) results.firstOrNull()?.rawValue?.let(latestOnCode)
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed.get()) {
                runCatching {
                    val cameraProvider = future.get()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, analysis
                    )
                    if (latestShowPreview) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
                        )
                    }
                    provider = cameraProvider
                }
            }
        }, mainExecutor)

        onDispose {
            disposed.set(true)
            provider?.unbind(preview, analysis)
            provider = null
            analysis.clearAnalyzer()
            scanner.close()
            analyzerExecutor.shutdown()
        }
    }

    LaunchedEffect(provider, showPreview) {
        val cameraProvider = provider ?: return@LaunchedEffect
        runCatching {
            if (showPreview) {
                if (!cameraProvider.isBound(preview)) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview
                    )
                }
            } else if (cameraProvider.isBound(preview)) {
                cameraProvider.unbind(preview)
            }
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}
