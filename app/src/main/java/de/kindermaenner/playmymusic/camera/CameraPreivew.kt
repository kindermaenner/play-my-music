package de.kindermaenner.playmymusic.camera

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlin.math.min

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    isActive: Boolean,
    onValidResult: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanHandled by remember { mutableStateOf(false) }
    var scanReady by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            scanHandled = false
            scanReady = false
            delay(800)
            scanReady = true
        } else {
            scanReady = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    setBackgroundColor(android.graphics.Color.BLACK)
                }

                previewView.post {
                    val size = min(previewView.width, previewView.height)
                    previewView.layoutParams = previewView.layoutParams.apply {
                        width = size
                        height = size
                    }
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    cameraProvider.unbindAll()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val barcodeScanner = BarcodeScanning.getClient()

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        if (!isActive || scanHandled || !scanReady) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstOrNull()?.rawValue

                                    if (value != null) {
                                        scanHandled = true
                                        onValidResult(value)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysis
                    )

                    Log.d("CameraX", "CameraPreview started")
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        if (!scanReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black)
            )
        }
    }
}