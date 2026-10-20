package com.zoya.voice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.zoya.voice.ui.theme.ZoyaVoiceTheme
import com.zoya.voice.ui.ZoyaAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZoyaVoiceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ZoyaAppRoot()
                }
            }
        }
    }
}