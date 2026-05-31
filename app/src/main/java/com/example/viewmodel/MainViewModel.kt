package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScanRepository
import com.example.data.ScanResult
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ScanRepository) : ViewModel() {

    val allScans: StateFlow<List<ScanResult>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onBarcodeScanned(barcode: Barcode) {
        val rawValue = com.example.qr.QRDecoderHelper.decodeBarcodeValue(barcode)
        if (rawValue.isEmpty()) return
        val now = System.currentTimeMillis()
        val detected = com.example.qr.QRTypeDetector.detect(rawValue)
        val newScan = ScanResult(
            rawValue = rawValue,
            format = barcode.format,
            type = barcode.valueType,
            timestamp = now,
            isFavorite = false,
            rawContent = rawValue,
            detectedType = detected.type.name,
            createdAt = now,
            favoriteAt = 0L
        )
        viewModelScope.launch {
            repository.insert(newScan)
        }
    }

    fun toggleFavorite(id: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(id, !isFav)
        }
    }
    
    fun deleteScan(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}

class MainViewModelFactory(private val repository: ScanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
