package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.CameraPreviewComponent
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

enum class CameraAspectRatio(val label: String) {
    FULL("Full"),
    ONE_ONE("1:1"),
    THREE_FOUR("3:4"),
    NINE_SIXTEEN("9:16")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onBarcodeScanned: (Barcode) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCreator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var torchEnabled by remember { mutableStateOf(false) }
    var currentRatio by remember { mutableStateOf(CameraAspectRatio.FULL) }
    var isRatioMenuExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isLocked by remember { mutableStateOf(false) }

    var activeBarcodeState by remember { mutableStateOf<Barcode?>(null) }
    var activeImageSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var activeCropRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    var activeRotation by remember { mutableStateOf<Int>(0) }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    val onBarcodeTrackingUpdate = { barcode: Barcode?, width: Int, height: Int, cropRect: android.graphics.Rect?, rotation: Int ->
        if (!isLocked) {
            if (barcode != null) {
                resetJob?.cancel()
                activeBarcodeState = barcode
                activeImageSize = Pair(width, height)
                activeCropRect = cropRect
                activeRotation = rotation
                
                resetJob = scope.launch {
                    delay(500) // 500ms grace period before returning to center
                    activeBarcodeState = null
                }
            }
        }
    }

    // Launcher to select image from gallery and scan barcodes
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            onBarcodeScanned(barcodes.first())
                        } else {
                            Toast.makeText(context, "Коды на изображении не найдены", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Не удалось распознать изображение", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val ratioModifier = when (currentRatio) {
                CameraAspectRatio.FULL -> Modifier.fillMaxSize()
                CameraAspectRatio.ONE_ONE -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                CameraAspectRatio.THREE_FOUR -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                CameraAspectRatio.NINE_SIXTEEN -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            }

            Box(
                modifier = ratioModifier,
                contentAlignment = Alignment.Center
            ) {
                // 1. Viewfinder (Full Screen or Center Aspect Ratio Cropped)
                CameraPreviewComponent(
                    onBarcodeScanned = { barcode ->
                        if (!isLocked) {
                            isLocked = true
                            activeBarcodeState = barcode
                            // Trigger haptic vibration on dynamic lock-on
                            try {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(75)
                                }
                            } catch (e: Exception) {}

                            // Smooth transition pause to let user see scan confirmed effect
                            scope.launch {
                                delay(400)
                                onBarcodeScanned(barcode)
                            }
                        }
                    },
                    onBarcodeTracking = onBarcodeTrackingUpdate,
                    torchEnabled = torchEnabled,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. High-end active dynamic tracking corner overlay
                ScannerOverlay(
                    activeBarcode = activeBarcodeState,
                    imageWidth = activeImageSize?.first ?: 0,
                    imageHeight = activeImageSize?.second ?: 0,
                    cropRect = activeCropRect,
                    rotation = activeRotation,
                    isLocked = isLocked,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. UI overlays on top of the live camera feed
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (Floating semi-transparent UI with aspect ratio control)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left aspect ratio selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        GlassIconButton(
                            onClick = { isRatioMenuExpanded = !isRatioMenuExpanded },
                            isDark = true,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("aspect_ratio_button")
                        ) {
                            Text(
                                text = currentRatio.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = isRatioMenuExpanded,
                            enter = expandHorizontally(
                                expandFrom = Alignment.Start,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(),
                            exit = shrinkHorizontally(
                                shrinkTowards = Alignment.Start,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            ) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CameraAspectRatio.values().forEach { ratio ->
                                    val isSelected = ratio == currentRatio
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.25f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                currentRatio = ratio
                                                isRatioMenuExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .testTag("ratio_option_${ratio.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = ratio.label,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Right Action: QR Generator
                    GlassIconButton(
                        onClick = onNavigateToCreator,
                        isDark = true,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("qr_creator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Создать QR",
                            tint = Color.White
                        )
                    }
                }

                // Empty space for target square visibility
                Spacer(modifier = Modifier.weight(1f))

                // Bottom Floating Control Bar (Floating above full-screen camera preview)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .padding(horizontal = 4.dp),
                    cornerRadius = 42.dp,
                    isDark = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flashlight Toggle
                        GlassIconButton(
                            onClick = { torchEnabled = !torchEnabled },
                            isDark = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Фонарик",
                                tint = if (torchEnabled) Color(0xFFFBBF24) else Color.White
                            )
                        }

                        // Scan Image Button
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { galleryLauncher.launch("image/*") }
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Выбрать фото",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Выбрать фото",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // History Navigate Button
                        GlassIconButton(
                            onClick = onNavigateToHistory,
                            isDark = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "История",
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    } else {
        // Permission Denied View styled with micro-glass details
        GlassBackground(isDark = true) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    cornerRadius = 32.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Требуется доступ к камере",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нам необходим доступ к камере для мгновенного сканирования кодов.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1E1B4B)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Предоставить доступ", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay(
    activeBarcode: Barcode?,
    imageWidth: Int,
    imageHeight: Int,
    cropRect: android.graphics.Rect?,
    rotation: Int,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        
        // Setup a beautiful minimalist target box (65% width) in the middle of the screen
        val boxWidth = screenWidth * 0.65f
        val boxHeight = boxWidth
        
        val left = (screenWidth - boxWidth) / 2
        val top = (screenHeight - boxHeight) / 2
        
        // Define Idle reference Corners
        val idleTL = Offset(left, top)
        val idleTR = Offset(left + boxWidth, top)
        val idleBR = Offset(left + boxWidth, top + boxHeight)
        val idleBL = Offset(left, top + boxHeight)

        var targetCorners = listOf(idleTL, idleTR, idleBR, idleBL)

        if (activeBarcode != null && imageWidth > 0 && imageHeight > 0) {
            val mapped = com.example.qr.QRDecoderHelper.mapPointsToScreen(
                barcode = activeBarcode,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                cropRect = cropRect,
                rotation = rotation,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
            if (mapped.size >= 4) {
                targetCorners = sortScreenCorners(mapped)
            }
        }

        // Animated target corners
        val animatedTL by animateOffsetAsState(
            targetValue = targetCorners[0],
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        )
        val animatedTR by animateOffsetAsState(
            targetValue = targetCorners[1],
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        )
        val animatedBR by animateOffsetAsState(
            targetValue = targetCorners[2],
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        )
        val animatedBL by animateOffsetAsState(
            targetValue = targetCorners[3],
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        )

        // Bouncy pulse scaling for confirmation lock-on
        val bounceScale by animateFloatAsState(
            targetValue = if (isLocked) 1.08f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )

        val alphaGlow by animateFloatAsState(
            targetValue = if (isLocked) 0.8f else if (activeBarcode != null) 0.25f else 0.08f,
            animationSpec = tween(300)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(screenWidth / 2f, screenHeight / 2f)

            // Scaled Animated Corners
            val currentTL = scaleOffset(animatedTL, center, bounceScale)
            val currentTR = scaleOffset(animatedTR, center, bounceScale)
            val currentBR = scaleOffset(animatedBR, center, bounceScale)
            val currentBL = scaleOffset(animatedBL, center, bounceScale)

            val cornerRadius = 24.dp.toPx()
            val borderLength = 32.dp.toPx()
            val strokeGlowWidth = 10.dp.toPx()
            val strokeSharpWidth = 4.dp.toPx()

            // 1. Solid bounding trace
            val fullQuadPath = Path().apply {
                moveTo(currentTL.x, currentTL.y)
                lineTo(currentTR.x, currentTR.y)
                lineTo(currentBR.x, currentBR.y)
                lineTo(currentBL.x, currentBL.y)
                close()
            }

            if (activeBarcode != null || isLocked) {
                val fillCol = if (isLocked) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
                drawPath(
                    path = fullQuadPath,
                    color = fillCol
                )
            }

            // 2. Vector custom corner brackets path
            val cornerPath = Path()

            // --- Top Left Corner Bracket ---
            val tlH = getPointAtDistance(currentTL, currentTR, borderLength)
            val tlHC = getPointAtDistance(currentTL, currentTR, cornerRadius)
            val tlV = getPointAtDistance(currentTL, currentBL, borderLength)
            val tlVC = getPointAtDistance(currentTL, currentBL, cornerRadius)
            cornerPath.moveTo(tlV.x, tlV.y)
            cornerPath.lineTo(tlVC.x, tlVC.y)
            cornerPath.quadraticTo(currentTL.x, currentTL.y, tlHC.x, tlHC.y)
            cornerPath.lineTo(tlH.x, tlH.y)

            // --- Top Right Corner Bracket ---
            val trH = getPointAtDistance(currentTR, currentTL, borderLength)
            val trHC = getPointAtDistance(currentTR, currentTL, cornerRadius)
            val trV = getPointAtDistance(currentTR, currentBR, borderLength)
            val trVC = getPointAtDistance(currentTR, currentBR, cornerRadius)
            cornerPath.moveTo(trV.x, trV.y)
            cornerPath.lineTo(trVC.x, trVC.y)
            cornerPath.quadraticTo(currentTR.x, currentTR.y, trHC.x, trHC.y)
            cornerPath.lineTo(trH.x, trH.y)

            // --- Bottom Right Corner Bracket ---
            val brH = getPointAtDistance(currentBR, currentBL, borderLength)
            val brHC = getPointAtDistance(currentBR, currentBL, cornerRadius)
            val brV = getPointAtDistance(currentBR, currentTR, borderLength)
            val brVC = getPointAtDistance(currentBR, currentTR, cornerRadius)
            cornerPath.moveTo(brV.x, brV.y)
            cornerPath.lineTo(brVC.x, brVC.y)
            cornerPath.quadraticTo(currentBR.x, currentBR.y, brHC.x, brHC.y)
            cornerPath.lineTo(brH.x, brH.y)

            // --- Bottom Left Corner Bracket ---
            val blH = getPointAtDistance(currentBL, currentBR, borderLength)
            val blHC = getPointAtDistance(currentBL, currentBR, cornerRadius)
            val blV = getPointAtDistance(currentBL, currentTL, borderLength)
            val blVC = getPointAtDistance(currentBL, currentTL, cornerRadius)
            cornerPath.moveTo(blV.x, blV.y)
            cornerPath.lineTo(blVC.x, blVC.y)
            cornerPath.quadraticTo(currentBL.x, currentBL.y, blHC.x, blHC.y)
            cornerPath.lineTo(blH.x, blH.y)

            // Draw Heavy Translucent Outer Neon Glow
            drawPath(
                path = cornerPath,
                color = Color.White.copy(alpha = alphaGlow),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeGlowWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Draw Core Sharp White Lines
            drawPath(
                path = cornerPath,
                color = Color.White,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeSharpWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}

private fun sortScreenCorners(points: List<Offset>): List<Offset> {
    if (points.size < 4) return points
    val sortedByY = points.sortedBy { it.y }
    val topTwo = sortedByY.take(2).sortedBy { it.x }
    val bottomTwo = sortedByY.drop(2).sortedBy { it.x }
    return listOf(topTwo[0], topTwo[1], bottomTwo[1], bottomTwo[0])
}

private fun getPointAtDistance(from: Offset, towards: Offset, distance: Float): Offset {
    val dx = towards.x - from.x
    val dy = towards.y - from.y
    val currentDist = kotlin.math.hypot(dx, dy)
    if (currentDist <= 0.001f) return from
    val ratio = distance / currentDist
    return Offset(from.x + dx * ratio, from.y + dy * ratio)
}

private fun scaleOffset(point: Offset, center: Offset, scale: Float): Offset {
    val dx = point.x - center.x
    val dy = point.y - center.y
    return Offset(center.x + dx * scale, center.y + dy * scale)
}
