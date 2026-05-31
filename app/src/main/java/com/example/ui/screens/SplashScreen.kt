package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Navigate after 2.8 seconds
    LaunchedEffect(Unit) {
        delay(2800)
        onTimeout()
    }

    // Infinite scan line animation Y-ratio (from 0.05f to 0.95f)
    val infiniteTransition = rememberInfiniteTransition(label = "SplashScreenScanner")
    
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanProgress"
    )

    // Pulse animation for QR elements
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Scanning Logo Frame
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                ) {
                    val w = size.width
                    val h = size.height
                    
                    // Standard drawing values based on dynamic bounds
                    val scaleFactor = logoScale
                    val baseW = w * 0.70f
                    val baseH = h * 0.70f
                    val left = (w - baseW) / 2f
                    val top = (h - baseH) / 2f
                    
                    // Viewfinder Outer Corner Brackets (White glowing)
                    val bracketLen = baseW * 0.16f
                    val bracketRad = baseW * 0.12f
                    val strokeW = 4.dp.toPx()
                    
                    // Apply interactive scale to code area
                    val centerPt = Offset(w / 2f, h / 2f)
                    
                    fun scaleOffsetVal(pt: Offset): Offset {
                        val dx = pt.x - centerPt.x
                        val dy = pt.y - centerPt.y
                        return Offset(centerPt.x + dx * scaleFactor, centerPt.y + dy * scaleFactor)
                    }

                    // Raw coordinates
                    val cornerTL = Offset(left, top)
                    val cornerTR = Offset(left + baseW, top)
                    val cornerBR = Offset(left + baseW, top + baseH)
                    val cornerBL = Offset(left, top + baseH)

                    // Scaled coordinates
                    val sTL = scaleOffsetVal(cornerTL)
                    val sTR = scaleOffsetVal(cornerTR)
                    val sBR = scaleOffsetVal(cornerBR)
                    val sBL = scaleOffsetVal(cornerBL)

                    // Top Left Bracket Path
                    val pathTL = Path().apply {
                        moveTo(sTL.x, sTL.y + bracketLen)
                        quadraticTo(sTL.x, sTL.y, sTL.x + bracketRad, sTL.y)
                        lineTo(sTL.x + bracketLen, sTL.y)
                    }
                    // Top Right Bracket Path
                    val pathTR = Path().apply {
                        moveTo(sTR.x - bracketLen, sTR.y)
                        quadraticTo(sTR.x, sTR.y, sTR.x, sTR.y + bracketRad)
                        lineTo(sTR.x, sTR.y + bracketLen)
                    }
                    // Bottom Right Bracket Path
                    val pathBR = Path().apply {
                        moveTo(sBR.x, sBR.y - bracketLen)
                        quadraticTo(sBR.x, sBR.y, sBR.x - bracketRad, sBR.y)
                        lineTo(sBR.x - bracketLen, sBR.y)
                    }
                    // Bottom Left Bracket Path
                    val pathBL = Path().apply {
                        moveTo(sBL.x + bracketLen, sBL.y)
                        quadraticTo(sBL.x, sBL.y, sBL.x, sBL.y - bracketRad)
                        lineTo(sBL.x, sBL.y - bracketLen)
                    }

                    // Draw all 4 Brackets
                    val strokeCol = Color.White
                    val strokeStyle = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawPath(pathTL, strokeCol, style = strokeStyle)
                    drawPath(pathTR, strokeCol, style = strokeStyle)
                    drawPath(pathBR, strokeCol, style = strokeStyle)
                    drawPath(pathBL, strokeCol, style = strokeStyle)

                    // Inner QR Code Representation
                    // Inside bounds (leaving margin)
                    val qrPadding = baseW * 0.12f
                    val qrLeft = left + qrPadding
                    val qrTop = top + qrPadding
                    val qrW = baseW - (qrPadding * 2f)
                    val qrH = baseH - (qrPadding * 2f)

                    val qrTL = scaleOffsetVal(Offset(qrLeft, qrTop))
                    val qrTR = scaleOffsetVal(Offset(qrLeft + qrW, qrTop))
                    val qrBL = scaleOffsetVal(Offset(qrLeft, qrTop + qrH))

                    val finderSize = qrW * 0.32f * scaleFactor
                    val qrColor = Color.White.copy(alpha = 0.95f)

                    // Helper to draw QR Finder Pattern
                    fun drawFinderPattern(topLeft: Offset, outerSize: Float) {
                        val strokePixel = outerSize * 0.125f
                        // Outer ring
                        drawRect(
                            color = qrColor,
                            topLeft = topLeft,
                            size = Size(outerSize, outerSize),
                            style = Stroke(width = strokePixel)
                        )
                        // Inner center filled square
                        val innerOffset = outerSize * 0.28f
                        val innerSize = outerSize * 0.44f
                        drawRect(
                            color = qrColor,
                            topLeft = Offset(topLeft.x + innerOffset, topLeft.y + innerOffset),
                            size = Size(innerSize, innerSize)
                        )
                    }

                    // Draw the 3 Primary Finder Patterns
                    drawFinderPattern(qrTL, finderSize)
                    drawFinderPattern(Offset(qrTR.x - finderSize, qrTR.y), finderSize)
                    drawFinderPattern(Offset(qrBL.x, qrBL.y - finderSize), finderSize)

                    // Draw stylized mock pixels / modules inside center/right/bottom areas
                    val pixelSize = qrW * 0.08f * scaleFactor
                    val cellsCount = 10
                    val startX = qrLeft
                    val startY = qrTop
                    val cellRawW = qrW / cellsCount
                    val cellRawH = qrH / cellsCount

                    // Deterministic mock QR code layout
                    val activePixels = listOf(
                        Pair(5, 1), Pair(6, 1), Pair(5, 3), Pair(8, 3), Pair(5, 4), Pair(7, 4),
                        Pair(1, 5), Pair(3, 5), Pair(4, 5), Pair(6, 5), Pair(8, 5), Pair(9, 5),
                        Pair(2, 6), Pair(5, 6), Pair(8, 6), Pair(1, 8), Pair(2, 8), Pair(1, 9), Pair(2, 9),
                        Pair(5, 8), Pair(6, 8), Pair(5, 9), Pair(6, 9), Pair(8, 8), Pair(9, 8), Pair(8, 9), Pair(9, 9)
                    )

                    activePixels.forEach { (cx, cy) ->
                        // Only draw if not colliding with finder patterns
                        // Finder TL is (0 to 3, 0 to 3)
                        // Finder TR is (6 to 9, 0 to 3)
                        // Finder BL is (0 to 3, 6 to 9)
                        val isFinder = (cx < 4 && cy < 4) || (cx >= 6 && cy < 4) || (cx < 4 && cy >= 6)
                        if (!isFinder) {
                            val rawPos = Offset(startX + cx * cellRawW + (cellRawW - pixelSize) / 2f, startY + cy * cellRawH + (cellRawH - pixelSize) / 2f)
                            val scaledPos = scaleOffsetVal(rawPos)
                            drawRect(
                                color = qrColor,
                                topLeft = scaledPos,
                                size = Size(pixelSize, pixelSize)
                            )
                        }
                    }

                    // --- Real-time Scanning Laser Line Animation ---
                    // Compute absolute layout-based Y position
                    val activeHeightY = sBL.y - sTL.y
                    val laserY = sTL.y + (activeHeightY * scanProgress)
                    val laserLeftX = sTL.x
                    val laserRightX = sTR.x

                    // Laser Color Scheme (Electric Gradient Aura matching Launcher Icon)
                    val laserHighlight = Color(0xFF909090)
                    val laserCore = Color(0xFFE0E0E0)
                    val laserStrokeWidth = 3.dp.toPx()

                    // Horizontal laser line core
                    drawLine(
                        color = laserCore,
                        start = Offset(laserLeftX, laserY),
                        end = Offset(laserRightX, laserY),
                        strokeWidth = laserStrokeWidth
                    )

                    // Glowing gradient aura below or around the scanning line to produce high-end feedback
                    val auraHeight = 16.dp.toPx()
                    val auraBrush = Brush.verticalGradient(
                        colors = listOf(
                            laserHighlight.copy(alpha = 0.28f),
                            laserHighlight.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                    
                    // Draw glowing shadow below the bar
                    drawRect(
                        brush = auraBrush,
                        topLeft = Offset(laserLeftX, laserY),
                        size = Size(laserRightX - laserLeftX, auraHeight)
                    )
                    
                    // Also draw a small glowing shadow above the bar for symmetrical flare
                    val auraBrushUp = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            laserHighlight.copy(alpha = 0.08f),
                            laserHighlight.copy(alpha = 0.28f)
                        )
                    )
                    drawRect(
                        brush = auraBrushUp,
                        topLeft = Offset(laserLeftX, laserY - auraHeight),
                        size = Size(laserRightX - laserLeftX, auraHeight)
                    )
                }
            }
        }
    }
}
