package com.example

import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.CreatorScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    companion object {
        var isFirstInRunInProcess = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setHighRefreshRate()
        setContent {
            MyApplicationTheme {
                QrApp(
                    viewModelFactory = MainViewModelFactory((application as ScanApplication).repository)
                )
            }
        }
    }

    private fun setHighRefreshRate() {
        try {
            val layoutParams = window.attributes
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val display = this.display
                val supportedModes = display?.supportedModes
                var bestMode: android.view.Display.Mode? = null
                var maxRate = 60f
                if (supportedModes != null) {
                    for (mode in supportedModes) {
                        if (mode.refreshRate > maxRate) {
                            maxRate = mode.refreshRate
                            bestMode = mode
                        }
                    }
                }
                if (bestMode != null) {
                    layoutParams.preferredDisplayModeId = bestMode.modeId
                } else {
                    layoutParams.preferredRefreshRate = 120f
                }
            } else {
                @Suppress("DEPRECATION")
                layoutParams.preferredRefreshRate = 120f
            }
            window.attributes = layoutParams
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}

@Composable
fun QrApp(viewModelFactory: MainViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
    val context = androidx.compose.ui.platform.LocalContext.current

    // Check if this is the first execution in operational memory (RAM) of this process
    val isFirstRun = remember {
        val firstRunFlags = MainActivity.isFirstInRunInProcess
        MainActivity.isFirstInRunInProcess = false
        firstRunFlags
    }

    val startDest = if (isFirstRun) "splash" else "scanner"

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate("scanner") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("scanner") {
            ScannerScreen(
                onBarcodeScanned = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                    
                    // Vibrate
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        val vibrator = vibratorManager.defaultVibrator
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                    
                    val decodedValue = com.example.qr.QRDecoderHelper.decodeBarcodeValue(barcode)
                    val encodedValue = URLEncoder.encode(decodedValue, StandardCharsets.UTF_8.toString())
                    navController.navigate("result/${encodedValue}/${barcode.valueType}") {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                },
                onNavigateToCreator = {
                    navController.navigate("creator")
                }
            )
        }
        
        composable("creator") {
            CreatorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("history") {
            val scans by viewModel.allScans.collectAsStateWithLifecycle()
            HistoryScreen(
                scans = scans,
                onBack = { navController.popBackStack() },
                onItemClick = { scan ->
                    val encodedValue = URLEncoder.encode(scan.rawValue, StandardCharsets.UTF_8.toString())
                    navController.navigate("result/${encodedValue}/${scan.type}")
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onDelete = viewModel::deleteScan
            )
        }
        
        composable("result/{rawValue}/{type}") { backStackEntry ->
            val rawValue = backStackEntry.arguments?.getString("rawValue") ?: ""
            val type = backStackEntry.arguments?.getString("type")?.toIntOrNull() ?: 0
            
            ResultScreen(
                rawValue = java.net.URLDecoder.decode(rawValue, StandardCharsets.UTF_8.toString()),
                type = type,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
