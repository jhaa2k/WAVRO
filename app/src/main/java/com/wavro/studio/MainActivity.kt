package com.wavro.studio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.wavro.studio.app.WavroApplication
import com.wavro.studio.ui.WavroApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display (transparent status/navigation bars)
        enableEdgeToEdge()
        
        setContent {
            // Claude Dark Theme Color Palette
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD97757), // AC
                    background = Color(0xFF262624), // BG
                    surface = Color(0xFF30302E), // PN
                    onPrimary = Color.White,
                    onBackground = Color(0xFFFAF9F5), // TX
                    onSurface = Color(0xFFFAF9F5) // TX
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- Runtime Permission Handler ---
                    val context = LocalContext.current
                    
                    // Determine which permissions to ask for based on Android version
                    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Android 13+ (API 33+) uses READ_MEDIA_AUDIO
                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        // Android 12 and below uses READ/WRITE_EXTERNAL_STORAGE
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        val allGranted = permissionsMap.values.all { it }
                        if (!allGranted) {
                            // Handle denied permissions (optional)
                            // You could show a dialog explaining why the app needs access
                        }
                    }

                    // Check and request permissions as soon as the app opens
                    LaunchedEffect(Unit) {
                        val hasAllPermissions = permissionsToRequest.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (!hasAllPermissions) {
                            permissionLauncher.launch(permissionsToRequest)
                        }
                    }
                    // -----------------------------------

                    // Launch the main application UI
                    WavroApp(application as WavroApplication)
                }
            }
        }
    }
}