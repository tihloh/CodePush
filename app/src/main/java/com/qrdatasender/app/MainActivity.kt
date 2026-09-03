package com.qrdatasender.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrdatasender.app.ui.QrDataSenderApp
import com.qrdatasender.app.ui.QrTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
            QrTheme(darkTheme = darkTheme) { QrDataSenderApp(viewModel) }
        }
    }
}
