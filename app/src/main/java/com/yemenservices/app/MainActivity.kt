package com.yemenservices.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.yemenservices.app.ui.AppViewModel
import com.yemenservices.app.ui.MainAppScaffold

class MainActivity : ComponentActivity() {
    
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        // Enable edge to edge full bleed drawings
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                MainAppScaffold(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}
