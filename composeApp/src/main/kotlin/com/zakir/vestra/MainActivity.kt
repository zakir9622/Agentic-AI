package com.zakir.vestra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zakir.vestra.ui.VestraNavHost
import com.zakir.vestra.ui.theme.VestraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as VestraApp
        setContent {
            VestraTheme {
                VestraNavHost(
                    appSettings = app.appSettings,
                    engineRouter = app.engineRouter,
                    wardrobe = app.wardrobe,
                    packManager = app.packManager,
                    reportQueue = app.reportQueue,
                    studioModels = app.studioModels,
                    garmentGuard = app.garmentGuard,
                )
            }
        }
    }
}
