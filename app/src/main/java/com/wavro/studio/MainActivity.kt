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
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD97757),
                    background = Color(0xFF262624),
                    surface = Color(0xFF30302E),
                    onPrimary = Color.White,
                    onBackground = Color(0xFFFAF9F5),
                    onSurface = Color(0xFFFAF9F5)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- Permission Handler ---
                    val context = LocalContext.current
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        // You could handle denied permissions here if needed
                        val allGranted = permissionsMap.values.all { it }
                        if (!allGranted) {
                            // Optional: Show a toast or dialog explaining why permissions are needed
                        }
                    }

                    LaunchedEffect(Unit) {
                        val hasPermission = permissions.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }
                        if (!hasPermission) {
                            permissionLauncher.launch(permissions)
                        }
                    }
                    // -------------------------

                    WavroApp(application as WavroApplication)
                }
            }
        }
    }
}