package com.kizqr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizqr.app.data.ThemeMode
import com.kizqr.app.ui.navigation.KizQrApp
import com.kizqr.app.ui.theme.KizQrTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KizQrApp
        setContent {
            val themeMode by app.settingsRepository.themeMode
                .collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dynamicColor by app.settingsRepository.dynamicColor
                .collectAsStateWithLifecycle(false)
            KizQrTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                KizQrApp()
            }
        }
    }
}
