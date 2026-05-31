package com.example.qr

import com.google.mlkit.vision.barcode.common.Barcode
import java.nio.charset.StandardCharsets

object QRDecoderHelper {

    /**
     * Decodes the barcode value to a correct UTF-8 or Windows-1251 string,
     * correcting any common Mojibake errors (e.g. UTF-8 interpreted as ISO-8859-1).
     */
    fun decodeBarcodeValue(barcode: Barcode): String {
        val rawBytes = barcode.rawBytes
        val rawValue = barcode.rawValue ?: ""

        if (rawBytes != null && rawBytes.isNotEmpty()) {
            try {
                // 1. Try decoding rawBytes as UTF-8
                if (isValidUtf8(rawBytes)) {
                    val utf8String = String(rawBytes, StandardCharsets.UTF_8)
                    return salvageMangledString(utf8String)
                } else {
                    // 2. If not valid UTF-8, try Windows-1251 (widely used in Russian legacy QR codes)
                    val win1251Charset = java.nio.charset.Charset.forName("windows-1251")
                    val win1251String = String(rawBytes, win1251Charset)
                    if (win1251String.any { it.code in 0x0400..0x04FF }) {
                        return win1251String
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        // If no raw bytes are available or decoding failed, salvage the raw string directly
        return salvageMangledString(rawValue)
    }

    /**
     * Detects if bytes represent a valid UTF-8 sequence.
     */
    fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b < 128) {
                i++
                continue
            }
            val needed = when {
                b in 192..223 -> 1
                b in 224..239 -> 2
                b in 240..247 -> 3
                else -> return false
            }
            if (i + needed >= bytes.size) return false
            for (j in 1..needed) {
                val cb = bytes[i + j].toInt() and 0xFF
                if (cb !in 128..191) return false
            }
            i += needed + 1
        }
        return true
    }

    /**
     * Salvages a string that has been mangled by decoding UTF-8 as ISO-8859-1.
     * Often, Russian and Emoji characters are mangled as Windows-1252 or ISO-8859-1.
     */
    fun salvageMangledString(mangled: String): String {
        if (mangled.isEmpty()) return mangled
        try {
            // Check if string contains typical characters resulting from UTF-8 to ISO-8859-1 mis-decoding
            // e.g. 'Ð', 'Ñ', 'â' (frequently occurring in Cyrillic UTF-8 mis-decoded strings).
            // Cyrillic ranges in UTF-8 begin with 0xD0 (208) or 0xD1 (209). In ISO-8859-1, 
            // these bytes map directly to Characters with codes 208 ('Ð') and 209 ('Ñ').
            if (mangled.contains('Ð') || mangled.contains('Ñ') || mangled.contains('â')) {
                val bytes = mangled.toByteArray(StandardCharsets.ISO_8859_1)
                val restored = String(bytes, StandardCharsets.UTF_8)
                if (restored.any { it.code in 0x0400..0x04FF || java.lang.Character.isSurrogate(it) }) {
                    return restored
                }
            }
        } catch (e: Exception) {
            // fall through
        }
        return mangled
    }

    /**
     * Maps the 4 corner points of a detected barcode from raw camera image pixels
     * to Jetpack Compose screen-space coordinates. Handles rotation, Crop rect from UseCaseGroup Viewport, and scaling.
     */
    fun mapPointsToScreen(
        barcode: Barcode,
        imageWidth: Int,
        imageHeight: Int,
        cropRect: android.graphics.Rect?,
        rotation: Int,
        screenWidth: Float,
        screenHeight: Float
    ): List<androidx.compose.ui.geometry.Offset> {
        if (imageWidth <= 0 || imageHeight <= 0 || screenWidth <= 0f || screenHeight <= 0f) {
            return emptyList()
        }

        val cornerPoints = barcode.cornerPoints ?: run {
            val rect = barcode.boundingBox ?: return emptyList()
            arrayOf(
                android.graphics.Point(rect.left, rect.top),
                android.graphics.Point(rect.right, rect.top),
                android.graphics.Point(rect.right, rect.bottom),
                android.graphics.Point(rect.left, rect.bottom)
            )
        }

        // Use cropRect from CameraX ViewPort, or fallback to the full uncropped image bounds
        val activeCrop = cropRect ?: android.graphics.Rect(0, 0, imageWidth, imageHeight)
        val cropW = activeCrop.width().toFloat()
        val cropH = activeCrop.height().toFloat()

        if (cropW <= 0f || cropH <= 0f) return emptyList()

        return cornerPoints.map { point ->
            // 1. Clamp and translate point to cropRect coordinates
            val xClamped = point.x.toFloat().coerceIn(activeCrop.left.toFloat(), activeCrop.right.toFloat())
            val yClamped = point.y.toFloat().coerceIn(activeCrop.top.toFloat(), activeCrop.bottom.toFloat())

            val xRel = xClamped - activeCrop.left
            val yRel = yClamped - activeCrop.top

            // 2. Rotate coordinates based on CameraX image rotation
            val (xRot, yRot, wRot, hRot) = when (rotation) {
                90 -> {
                    // Portrait mode (90 degrees clockwise)
                    val xr = cropH - yRel
                    val yr = xRel
                    Quad(xr, yr, cropH, cropW)
                }
                270 -> {
                    // Reverse portrait (270 degrees)
                    val xr = yRel
                    val yr = cropW - xRel
                    Quad(xr, yr, cropH, cropW)
                }
                180 -> {
                    // Landscape flipped (180 degrees)
                    val xr = cropW - xRel
                    val yr = cropH - yRel
                    Quad(xr, yr, cropW, cropH)
                }
                else -> { // 0 or general landscape
                    Quad(xRel, yRel, cropW, cropH)
                }
            }

            // 3. Scale precisely to screen space (compensating for any layout stretch)
            val scaleX = screenWidth / wRot
            val scaleY = screenHeight / hRot

            val xScreen = xRot * scaleX
            val yScreen = yRot * scaleY

            androidx.compose.ui.geometry.Offset(xScreen, yScreen)
        }
    }

    private data class Quad(val x: Float, val y: Float, val w: Float, val h: Float)
}
