package com.wavro.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wavro.studio.app.WavroApplication
import com.wavro.studio.ui.WavroApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scheme = darkColorScheme(
                primary = Color(0xFFD97757),
                background = Color(0xFF262624),
                surface = Color(0xFF30302E),
                onPrimary = Color.White,
                onBackground = Color(0xFFFAF9F5),
                onSurface = Color(0xFFFAF9F5)
            )
            MaterialTheme(colorScheme = scheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WavroApp(application as WavroApplication)
                }
            }
        }
    }
}