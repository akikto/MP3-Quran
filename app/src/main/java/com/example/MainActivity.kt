package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.QuranApp
import com.example.ui.theme.AudioQuranTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(7, 26, 20)),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(7, 26, 20))
        )
        setContent {
            AudioQuranTheme {
                QuranApp()
            }
        }
    }
}
