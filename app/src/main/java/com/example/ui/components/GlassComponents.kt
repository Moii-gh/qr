package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Soft glowing gradients for the Apple-style background
val AppleLiquidDarkGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F172A), // Slate 900
        Color(0xFF1E1B4B), // Indigo 950
        Color(0xFF180025)  // Deep Violet Black
    )
)

val AppleLiquidLightGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF1F5F9), // Slate 100
        Color(0xFFEEF2F6), // Cool gray/blue
        Color(0xFFE0E7FF)  // Soft Indigo 100
    )
)

@Composable
fun GlassBackground(
    isDark: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isPowerSaving = com.example.config.PerformanceSettings.isPowerSavingMode

    // Smooth slow infinite animations for lava lamp style
    val infiniteTransition = rememberInfiniteTransition(label = "LavaLampBackground")

    // Slow organic drift waves (periods of 24s, 34s, 44s, 54s)
    val float1 by if (isPowerSaving) {
        remember { mutableStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(24000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Blob1"
        )
    }

    val float2 by if (isPowerSaving) {
        remember { mutableStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(34000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Blob2"
        )
    }

    val float3 by if (isPowerSaving) {
        remember { mutableStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(44000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Blob3"
        )
    }

    val float4 by if (isPowerSaving) {
        remember { mutableStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(54000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Blob4"
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw solid dark base
            drawRect(color = Color(0xFF090A0C))

            // 2. Weak/Power Saving Mode: simplified, non-animated blurred background elements to keep CPU/GPU clean.
            if (isPowerSaving) {
                // Static, elegant, high-performance soft background layout
                // Blob 1: Static Top Right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4A4D54).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(width * 0.8f, height * 0.2f),
                        radius = Math.max(width, height) * 0.45f
                    ),
                    center = Offset(width * 0.8f, height * 0.2f),
                    radius = Math.max(width, height) * 0.45f
                )

                // Blob 2: Static Bottom Left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF33353B).copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(width * 0.2f, height * 0.8f),
                        radius = Math.max(width, height) * 0.5f
                    ),
                    center = Offset(width * 0.2f, height * 0.8f),
                    radius = Math.max(width, height) * 0.5f
                )
            } else {
                // 3. Normal / Premium Liquid Mode: slow moving blurred lava blobs
                val baseRadius = Math.max(width, height)
                
                // Blob 1: Slow circular motion (Top-Left area)
                val center1X = (width * 0.3f) + (width * 0.18f) * Math.cos(float1.toDouble()).toFloat()
                val center1Y = (height * 0.25f) + (height * 0.15f) * Math.sin(float1.toDouble()).toFloat()
                val radius1 = baseRadius * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF44474E).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(center1X, center1Y),
                        radius = radius1
                    ),
                    center = Offset(center1X, center1Y),
                    radius = radius1
                )

                // Blob 2: Slow drifting (Bottom-Right area)
                val center2X = (width * 0.75f) + (width * 0.15f) * Math.sin(float2.toDouble()).toFloat()
                val center2Y = (height * 0.72f) + (height * 0.18f) * Math.cos(float2.toDouble()).toFloat()
                val radius2 = baseRadius * 0.58f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF5D616B).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(center2X, center2Y),
                        radius = radius2
                    ),
                    center = Offset(center2X, center2Y),
                    radius = radius2
                )

                // Blob 3: Center-left breathing vertical stretch
                val center3X = (width * 0.2f) + (width * 0.12f) * Math.sin(float3.toDouble()).toFloat()
                val center3Y = (height * 0.6f) + (height * 0.2f) * Math.cos(float3.toDouble()).toFloat()
                val radius3 = baseRadius * 0.48f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2F3136).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(center3X, center3Y),
                        radius = radius3
                    ),
                    center = Offset(center3X, center3Y),
                    radius = radius3
                )

                // Blob 4: Small light shimmer in middle top right
                val center4X = (width * 0.85f) + (width * 0.1f) * Math.cos(float4.toDouble()).toFloat()
                val center4Y = (height * 0.4f) + (height * 0.12f) * Math.sin(float4.toDouble()).toFloat()
                val radius4 = baseRadius * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7A7E8A).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(center4X, center4Y),
                        radius = radius4
                    ),
                    center = Offset(center4X, center4Y),
                    radius = radius4
                )
            }
        }

        // 4. Matte Premium Tint & Contrast Overlay - Ensures readability is 100% crystal clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.48f),
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    isDark: Boolean = true,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.60f),
            if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.2.dp, borderColor, RoundedCornerShape(cornerRadius))
                } else {
                    Modifier.border(1.2.dp, borderBrush, RoundedCornerShape(cornerRadius))
                }
            ),
        content = content
    )
}

@Composable
fun GlassClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    isDark: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.60f),
            if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .clickable { onClick() }
            .border(1.2.dp, borderBrush, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    icon: @Composable () -> Unit
) {
    val glassColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.60f),
            if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.10f)
        )
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(glassColor)
            .clickable { onClick() }
            .border(1.dp, borderBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
