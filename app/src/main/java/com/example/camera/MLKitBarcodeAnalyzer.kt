package com.example.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MLKitBarcodeAnalyzer(
    private val onBarcodeScanned: (Barcode) -> Unit,
    private val onBarcodeTracking: (Barcode?, imageWidth: Int, imageHeight: Int, cropRect: android.graphics.Rect?, rotationDegrees: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(options)
    
    // To prevent multiple triggerings for the same frame or rapid scanning
    private var isScanning = false
    private var lastScannedValue = ""
    private var lastScanTime = 0L
    private var lastAnalysisTime = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Decreased to 60ms for smooth 16Hz tracking updates
        if (currentTime - lastAnalysisTime < 60) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            if (isScanning) {
                imageProxy.close()
                return
            }

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        // Choose nearest barcode to center of screen
                        val bestBarcode = barcodes.minByOrNull { bc ->
                            val r = bc.boundingBox
                            if (r != null) {
                                val dx = r.centerX() - image.width / 2
                                val dy = r.centerY() - image.height / 2
                                dx * dx + dy * dy
                            } else {
                                Int.MAX_VALUE
                            }
                        }

                        if (bestBarcode != null) {
                            onBarcodeTracking(
                                bestBarcode,
                                image.width,
                                image.height,
                                imageProxy.cropRect,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            val rawValue = bestBarcode.rawValue
                            val now = System.currentTimeMillis()
                            
                            // Check scanner cooldown
                            if (rawValue != null && (rawValue != lastScannedValue || now - lastScanTime > 2000)) {
                                isScanning = true
                                lastScannedValue = rawValue
                                lastScanTime = now
                                onBarcodeScanned(bestBarcode)
                                // Release scanning lock after cooldown to allow scanner reuse if navigating back
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    isScanning = false
                                }, 1500)
                            }
                        } else {
                            onBarcodeTracking(null, 0, 0, null, 0)
                        }
                    } else {
                        onBarcodeTracking(null, 0, 0, null, 0)
                    }
                }
                .addOnFailureListener {
                    onBarcodeTracking(null, 0, 0, null, 0)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
