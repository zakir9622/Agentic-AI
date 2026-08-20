package com.zakir.vestra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zakir.vestra.shared.settings.AppearanceMode
import com.zakir.vestra.ui.VestraNavHost
import com.zakir.vestra.ui.theme.VestraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as VestraApp
        setContent {
            val appearance by app.appSettings.appearanceMode.collectAsState()
            val dark = when (appearance) {
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            VestraTheme(darkTheme = dark) {
                VestraNavHost(
                    appSettings = app.appSettings,
                    engineRouter = app.engineRouter,
                    wardrobe = app.wardrobe,
                    packManager = app.packManager,
                    studioModels = app.studioModels,
                    generative = app.generative,
                    usageLedger = app.usageLedger,
                    freeCloudDiscovery = app.freeCloudDiscovery,
                )
            }
        }
    }
}
