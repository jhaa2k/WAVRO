package com.wavro.studio
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
class MainActivity:ComponentActivity(){override fun onCreate(s:Bundle?){super.onCreate(s)
 setContent{MaterialTheme(darkColorScheme(primary=Color(0xFF8B5CF6),secondary=Color(0xFF22D3EE),background=Color(0xFF0A0B11),surface=Color(0xFF11131C),surfaceVariant=Color(0xFF1B1E2C))){Surface(color=MaterialTheme.colorScheme.background){App()}}}}}
