package com.scannerpro.lectorqr.presentation.ui.scanner

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Camera
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.scannerpro.lectorqr.data.repository.BarcodeAnalyzer
import com.scannerpro.lectorqr.domain.model.BarcodeResult
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFlashEnabled: Boolean,
    isFrontCamera: Boolean,
    zoomRatio: Float,
    isAutofocusEnabled: Boolean = true,
    isTapToFocusEnabled: Boolean = true,
    cameraSelection: Int = 0,
    onZoomRangeChanged: (Float, Float) -> Unit = { _, _ -> },
    onBarcodeDetected: (com.google.mlkit.vision.barcode.common.Barcode, android.graphics.Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    
    // Maintain a single executor and clean up when the composable is destroyed
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Reference to the active camera to update zoom/torch without rebinding
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val previewView = remember { 
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Effect to bind/unbind camera when structural parameters change
    androidx.compose.runtime.LaunchedEffect(isFrontCamera, cameraSelection) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(scanner) { barcode, bitmap ->
                    onBarcodeDetected(barcode, bitmap)
                })
            }

        val cameraSelector = when {
            isFrontCamera -> CameraSelector.DEFAULT_FRONT_CAMERA
            cameraSelection == 1 -> CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            // Get zoom range and notify
            val zoomState = camera?.cameraInfo?.zoomState?.value
            val minZoom = zoomState?.minZoomRatio ?: 1.0f
            val maxZoom = zoomState?.maxZoomRatio ?: 10.0f
            onZoomRangeChanged(minZoom, maxZoom)
            
            // Apply initial zoom (clamped to range) and torch
            val initialZoom = zoomRatio.coerceIn(minZoom, maxZoom)
            camera?.cameraControl?.setZoomRatio(initialZoom)
            camera?.cameraControl?.enableTorch(isFlashEnabled)
            
            // Set up tap to focus
            if (isAutofocusEnabled && isTapToFocusEnabled) {
                previewView.setOnTouchListener { view, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                        view.performClick()
                    }
                    true
                }
            } else {
                previewView.setOnTouchListener(null)
            }
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }

    // Update zoomRatio dynamically without rebinding
    androidx.compose.runtime.LaunchedEffect(zoomRatio) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    // Update torch dynamically without rebinding
    androidx.compose.runtime.LaunchedEffect(isFlashEnabled) {
        camera?.cameraControl?.enableTorch(isFlashEnabled)
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView },
        update = { /* Updates are handled via LaunchedEffects above */ }
    )
}
