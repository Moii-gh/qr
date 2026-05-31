package com.example.qr

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector

data class QRAction(
    val label: String,
    val icon: ImageVector,
    val isPrimary: Boolean = false,
    val execute: (Context) -> Unit
)
