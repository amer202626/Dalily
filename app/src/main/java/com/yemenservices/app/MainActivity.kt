package com.yemenservices.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.yemenservices.app.ui.AppViewModel
import com.yemenservices.app.ui.MainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val colorScheme = if (isDarkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
            
            MaterialTheme(
                colorScheme = colorScheme
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
