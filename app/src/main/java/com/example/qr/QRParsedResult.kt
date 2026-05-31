package com.example.qr

data class QRParsedResult(
    val type: QRType,
    val rawValue: String,
    val title: String,
    val extractedData: Map<String, String> = emptyMap()
)
