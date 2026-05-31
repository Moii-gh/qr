package com.example.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.MLKitBarcodeAnalyzer
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.AspectRatio
import java.util.concurrent.Executors
import androidx.camera.core.Preview

@Composable
fun CameraPreviewComponent(
    onBarcodeScanned: (Barcode) -> Unit,
    onBarcodeTracking: (Barcode?, Int, Int, android.graphics.Rect?, Int) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(context) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(cameraProvider, previewView) {
        val provider = cameraProvider
        val view = previewView

        if (provider != null && view != null) {
            try {
                provider.unbindAll()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            analyzerExecutor,
                            MLKitBarcodeAnalyzer(onBarcodeScanned, onBarcodeTracking)
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val viewPort = view.viewPort
                if (viewPort != null) {
                    val useCaseGroup = UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(imageAnalyzer)
                        .setViewPort(viewPort)
                        .build()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        useCaseGroup
                    )
                } else {
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Use case binding failed", e)
            }
        }
    }
    
    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzerExecutor.shutdown()
        }
    }

    var currentZoomRatio by remember { mutableStateOf(1.0f) }
    var showZoomBadge by remember { mutableStateOf(false) }
    var hasZoomedOnce by remember { mutableStateOf(false) }

    DisposableEffect(camera) {
        val zoomStateLiveData = camera?.cameraInfo?.zoomState ?: return@DisposableEffect onDispose {}
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { state ->
            if (state != null) {
                currentZoomRatio = state.zoomRatio
            }
        }
        zoomStateLiveData.observeForever(observer)
        onDispose {
            zoomStateLiveData.removeObserver(observer)
        }
    }

    LaunchedEffect(currentZoomRatio) {
        if (hasZoomedOnce) {
            showZoomBadge = true
            delay(1500)
            showZoomBadge = false
        } else {
            hasZoomedOnce = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    previewView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera) {
                    val currentCamera = camera ?: return@pointerInput
                    detectTransformGestures { _, _, zoom, _ ->
                        val zoomState = currentCamera.cameraInfo.zoomState.value ?: return@detectTransformGestures
                        val minZoom = zoomState.minZoomRatio
                        val maxZoom = zoomState.maxZoomRatio
                        val targetZoomRatio = (currentZoomRatio * zoom).coerceIn(minZoom, maxZoom)
                        currentCamera.cameraControl.setZoomRatio(targetZoomRatio)
                    }
                }
        )

        // Floating Zoom Level Indicator Badge
        AnimatedVisibility(
            visible = showZoomBadge,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1fx", currentZoomRatio),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
